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
package com.codenvy.api.dao.mongo.ssh;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;
import org.eclipse.che.api.ssh.server.model.impl.SshPairImpl;

/**
 * @author Sergii Leschenko
 */
public class UsersSshPairCodec implements Codec<SshPairImpl> {
    private final Codec<Document> codec;

    public UsersSshPairCodec(CodecRegistry registry) {
        codec = registry.get(Document.class);
    }

    @Override
    public void encode(BsonWriter writer, SshPairImpl sshPair, EncoderContext encoderContext) {

        final Document document = new Document().append("owner", sshPair.getOwner())
                                                .append("service", sshPair.getService())
                                                .append("name", sshPair.getName())
                                                .append("publicKey", sshPair.getPublicKey())
                                                .append("privateKey", sshPair.getPrivateKey());
        codec.encode(writer, document, encoderContext);
    }

    @Override
    public Class<SshPairImpl> getEncoderClass() {
        return SshPairImpl.class;
    }

    @Override
    public SshPairImpl decode(BsonReader reader, DecoderContext decoderContext) {
        final Document document = codec.decode(reader, decoderContext);
        return new SshPairImpl(document.getString("owner"),
                                document.getString("service"),
                                document.getString("name"),
                                document.getString("publicKey"),
                                document.getString("privateKey"));
    }
}
