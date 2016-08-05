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

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

/**
 * Implementations of this abstract class can both encode and decode values of type {@code T} by {@link Document} instance.
 *
 * @author Sergii Leschenko
 */
public abstract class AbstractDocumentCodec<T> implements Codec<T> {
    protected final CodecRegistry   codecRegistry;
    private final   Codec<Document> documentCodec;

    protected AbstractDocumentCodec(CodecRegistry codecRegistry) {
        this.codecRegistry = codecRegistry;
        this.documentCodec = codecRegistry.get(Document.class);
    }

    @Override
    public T decode(BsonReader reader, DecoderContext decoderContext) {
        return decode(documentCodec.decode(reader, decoderContext));
    }

    @Override
    public void encode(BsonWriter writer, T value, EncoderContext encoderContext) {
        documentCodec.encode(writer, encode(value), encoderContext);
    }

    public abstract Document encode(T value);

    public abstract T decode(Document decode);
}
