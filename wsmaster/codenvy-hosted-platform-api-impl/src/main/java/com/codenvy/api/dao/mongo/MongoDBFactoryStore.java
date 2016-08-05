/*
 *  [2012] - [2016] Codenvy, S.A.
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.api.dao.mongo;


import com.google.common.base.Strings;
import com.mongodb.MongoException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.util.JSON;

import org.bson.Document;
import org.bson.types.Binary;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.factory.server.FactoryImage;
import org.eclipse.che.api.factory.server.FactoryStore;
import org.eclipse.che.api.factory.shared.dto.Factory;
import org.eclipse.che.commons.lang.NameGenerator;
import org.eclipse.che.commons.lang.Pair;
import org.eclipse.che.dto.server.DtoFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.eq;
import static java.lang.String.format;

/**
 * MongoDB implementation of {@link FactoryStore}
 * <p>
 * Storage schema:
 * <pre>
 * {
 *  "_id" : "ihb2r9ys1uk4lqxk",
 *  "factory" : {
 *                "v" : "4.0",
 *                "workspace" : {
 *                    ...
 *                },
 *                "ide" : {
 *                    "onProjectOpened" : {
 *                        "actions" : [
 *                        ...
 *                        ]
 *                     }
 *                },
 *                "creator" : {
 *                    "created" : 1448271625116,
 *                    "userId" : "user9s7lxyvk6eqzgb7t"
 *                }
 * },
 * "images" : [
 *              {
 *                "name" : "o9urwt58gnfjs11j",
 *                "type" : "image/jpeg",
 *                "data" : "<binary_data>"
 *               }
 *  ]
 * }
 *
 * </pre>
 *
 * @author Max Shaposhnik
 */

@Singleton
public class MongoDBFactoryStore implements FactoryStore {
    private static final Logger LOG = LoggerFactory.getLogger(MongoDBFactoryStore.class);

    /**
     * Escaped dot character as suggested by MongoDB
     */
    protected static final char ESCAPED_DOT = '\uFF0E';

    /**
     * Escaped dollar character as suggested by MongoDB
     */
    protected static final char ESCAPED_DOLLAR = '\uFF04';

    private final MongoCollection<Document> factories;

    @Inject
    public MongoDBFactoryStore(@Named("mongo.db.factory") MongoDatabase db,
                               @Named("factory.storage.db.collection") String collectionName) {
        factories = db.getCollection(collectionName, Document.class);
        factories.createIndex(new Document("_id", 1), new IndexOptions().unique(true));
        factories.createIndex(new Document("factory.name", 1).append("factory.creator.userId", 1), new IndexOptions().sparse(true));

    }


    @Override
    public String saveFactory(Factory factory, Set<FactoryImage> images) throws ConflictException, ServerException {
        if (factory == null) {
            throw new NullPointerException("The factory shouldn't be null");
        }
        validateFactoryName(factory);

        factory.setId(NameGenerator.generate("", 16));
        final List<Document> imageList;
        if (images == null) {
            imageList = Collections.emptyList();
        } else {
            imageList = images.stream()
                              .map(one -> new Document().append("name", one.getName())
                                                        .append("type", one.getMediaType())
                                                        .append("data", new Binary(one.getImageData())))
                              .collect(Collectors.toList());
        }

        final Document factoryDocument = Document.parse(encode(DtoFactory.getInstance().toJson(factory)));
        final Document storedData = new Document("_id", factory.getId()).append("factory", factoryDocument)
                                                                        .append("images", imageList);
        try {
            factories.insertOne(storedData);
        } catch (MongoException e) {
            throw new ServerException("Unable to store factory: " + e.getMessage(), e);
        }
        return factory.getId();
    }

    @Override
    public void removeFactory(String factoryId) throws NotFoundException, ServerException {
        if (factoryId == null) {
            throw new NullPointerException("The factory Id shouldn't be null");
        }

        try {
            if (factories.deleteOne(new Document("_id", factoryId)).getDeletedCount() == 0) {
                throw new NotFoundException("Factory with id '" + factoryId + "' was not found");
            }
        } catch (MongoException e) {
            throw new ServerException("Unable to remove factory " + factoryId, e);
        }
    }

    @Override
    public Factory getFactory(String factoryId) throws NotFoundException, ServerException {
        if (factoryId == null) {
            throw new NullPointerException("The factory Id shouldn't be null");
        }
        final FindIterable<Document> findIt = factories.find(new Document("_id", factoryId));
        if (findIt.first() == null) {
            throw new NotFoundException("Factory with id '" + factoryId + "' was not found");
        }
        Document res = findIt.first();
        String decoded = decode(JSON.serialize(res.get("factory", Document.class)));

        // Processing factory
        Factory factory = DtoFactory.getInstance().createDtoFromJson(decoded, Factory.class);

        factory.setId((String)res.get("_id"));

        return factory;
    }

    @Override
    public List<Factory> findByAttribute(int maxItems, int skipCount, List<Pair<String, String>> attributes)
            throws IllegalArgumentException {
        if (skipCount < 0) {
            throw new IllegalArgumentException("'skipCount' parameter is negative.");
        }

        final List<Factory> result = new ArrayList<>();
        Document query = new Document();
        for (Pair<String, String> one : attributes) {
            query.append(format("factory.%s", one.first), encode(one.second));
        }
        final FindIterable<Document> findIt = factories.find(query).skip(skipCount).limit(maxItems);
        for (Document one : findIt) {
            String decoded = decode(JSON.serialize(one.get("factory", Document.class)));
            Factory factory =
                    DtoFactory.getInstance().createDtoFromJson(decoded, Factory.class);
            factory.setId((String)one.get("_id"));
            result.add(factory);
        }
        return result;
    }

    @SuppressWarnings("unchecked") //"images" is always list of documents
    @Override
    public Set<FactoryImage> getFactoryImages(String factoryId, String imageId) throws NotFoundException {
        if (factoryId == null) {
            throw new NullPointerException("The images factory Id shouldn't be null");
        }

        final FindIterable<Document> findIt = factories.find(new Document("_id", factoryId));
        Document res = findIt.first();
        if (res == null) {
            throw new NotFoundException("Factory with id '" + factoryId + "' was not found");
        }
        final Set<FactoryImage> images = new HashSet<>();

        for (Document obj : (List<Document>)res.get("images")) {
            try {
                if (imageId == null || obj.get("name").equals(imageId)) {
                    FactoryImage image = new FactoryImage();
                    image.setName((String)obj.get("name"));
                    image.setMediaType((String)obj.get("type"));
                    image.setImageData(((Binary)obj.get("data")).getData());
                    images.add(image);
                }
            } catch (IOException e) {
                LOG.error("Wrong image data found for image " + obj.get("name"), e);
            }
        }
        return images;
    }

    @Override
    public String updateFactory(String factoryId, Factory factory) throws NotFoundException, ConflictException {
        if (factoryId == null) {
            throw new NullPointerException("The update factory Id shouldn't be null");
        }

        if (factory == null) {
            throw new NullPointerException("The factory replacement shouldn't be null");
        }

        // if name is changed, we must validate it
        if (factory.getName() != null) {
            FindIterable<Document> existing = factories.find(new Document("_id", factoryId));
            if (existing.first() != null && !factory.getName().equals(existing.first().get("factory", Document.class).get("name"))) {
                validateFactoryName(factory);
            }
        }

        factory.setId(factoryId);
        Document factoryReplacement = new Document("$set",
                                                   new Document("factory", JSON.parse(
                                                           encode(DtoFactory.getInstance()
                                                                            .toJson(factory)))));

        if (factories.findOneAndUpdate(eq("_id", factoryId), factoryReplacement) == null) {
            throw new NotFoundException("Factory with id '" + factoryId + "' was not found");
        }
        // return the factory ID
        return factory.getId();
    }

    /**
     * Validate factory name for uniqueness
     *
     * <p>Check that we don't have the factory with same name and creator.
     * This cannot be done using composite key and sparse + unique index
     * because it will check constraint even on single field, e.g. when name is null
     * see https://docs.mongodb.org/manual/core/index-sparse/#sparse-compound-indexes
     * This possible can be resolved after upgrade to Mongo 3.2 using partial indexes
     * see https://docs.mongodb.org/manual/core/index-partial/#index-type-partial
     * Sparse index is created to speed up this kind of search.
     *
     * @param factory factory to validate
     *
     * @throws ConflictException if there is a factory present with this name
     */
    private void validateFactoryName(Factory factory) throws ConflictException {
        if (Strings.isNullOrEmpty(factory.getName())) {
            return;
        }

        final FindIterable<Document> findIt =
                factories.find(new Document().append("factory.name", factory.getName())
                                             .append("factory.creator.userId", factory.getCreator().getUserId()));

        if (findIt.first() != null) {
            throw new ConflictException("You already have factory with given name. Please, choose another one.");
        }
    }

    /**
     * Mongodb is avoiding storage of dot and $ sign. Documentation is suggesting that we encode such characters
     * http://docs.mongodb.org/manual/reference/limits/#Restrictions-on-Field-Names
     * http://docs.mongodb.org/manual/faq/developers/#faq-dollar-sign-escaping
     *
     * @param value
     *         the value to encode
     * @return the encoded value
     */
    protected String encode(String value) {
        return value.replace('.', ESCAPED_DOT).replace('$', ESCAPED_DOLLAR);
    }

    /**
     * Decode the value
     *
     * @param value
     *         value to unescape
     * @return the original value without any encoding
     */
    protected String decode(String value) {
        return value.replace(ESCAPED_DOT, '.').replace(ESCAPED_DOLLAR, '$');
    }
}
