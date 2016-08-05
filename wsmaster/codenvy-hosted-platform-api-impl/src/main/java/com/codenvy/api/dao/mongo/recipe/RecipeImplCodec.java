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
package com.codenvy.api.dao.mongo.recipe;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;
import org.eclipse.che.api.machine.server.recipe.RecipeImpl;

import java.util.List;

/**
 * Encodes and decodes {@link RecipeImpl}EntryImp
 *
 * @author Sergii Leschenko
 */
public class RecipeImplCodec implements Codec<RecipeImpl> {

    private final Codec<Document>                     codec;

    public RecipeImplCodec(CodecRegistry registry) {
        this.codec = registry.get(Document.class);
    }

    @Override
    public RecipeImpl decode(BsonReader reader, DecoderContext decoderContext) {
        final Document document = codec.decode(reader, decoderContext);
        @SuppressWarnings("unchecked")//tags is always list of strings
        final List<String> tags = (List<String>)document.get("tags");

        return new RecipeImpl().withId(document.getString("_id"))
                               .withName(document.getString("name"))
                               .withCreator(document.getString("creator"))
                               .withType(document.getString("type"))
                               .withScript(document.getString("script"))
                               .withTags(tags);
    }

    @Override
    public void encode(BsonWriter writer, RecipeImpl recipe, EncoderContext encoderContext) {
        final Document document = new Document().append("_id", recipe.getId())
                                                .append("name", recipe.getName())
                                                .append("creator", recipe.getCreator())
                                                .append("script", recipe.getScript())
                                                .append("type", recipe.getType())
                                                .append("tags", recipe.getTags());

        codec.encode(writer, document, encoderContext);
    }

    @Override
    public Class<RecipeImpl> getEncoderClass() {
        return RecipeImpl.class;
    }
}
