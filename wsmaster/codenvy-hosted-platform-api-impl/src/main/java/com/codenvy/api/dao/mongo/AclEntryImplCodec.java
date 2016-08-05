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

import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.eclipse.che.api.machine.server.model.impl.AclEntryImpl;

import java.util.List;

/**
 * @author Sergii Leschenko
 */
public class AclEntryImplCodec extends AbstractDocumentCodec<AclEntryImpl> {
    public AclEntryImplCodec(CodecRegistry registry) {
        super(registry);
    }

    @Override
    public Document encode(AclEntryImpl entry) {
        return new Document().append("user", entry.getUser())
                             .append("actions", entry.getActions());
    }

    @Override
    public AclEntryImpl decode(Document document) {
        @SuppressWarnings("unchecked")//actions is always list of strings
        final List<String> actions = (List<String>)document.get("actions");
        return new AclEntryImpl(document.getString("user"), actions);
    }

    @Override
    public Class<AclEntryImpl> getEncoderClass() {
        return AclEntryImpl.class;
    }
}
