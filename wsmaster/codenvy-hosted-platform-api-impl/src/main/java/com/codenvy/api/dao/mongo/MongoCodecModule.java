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

import com.codenvy.api.dao.mongo.recipe.RecipeImplCodec;
import com.codenvy.api.dao.mongo.stack.StackImplCodec;
import com.codenvy.api.workspace.server.model.WorkerImpl;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;
import org.eclipse.che.api.machine.server.recipe.RecipeImpl;
import org.eclipse.che.api.workspace.server.model.impl.WorkspaceImpl;
import org.eclipse.che.api.workspace.server.model.impl.stack.StackImpl;
import org.eclipse.che.inject.DynaModule;

/**
 * Binds {@link CodecProvider} instances.
 *
 * <p>Any {@link Codec codec} instance may need another codec for encoding/decoding complex objects, actually
 * because of this {@link CodecProvider providers} used instead of simple {@link Codec codecs}.
 * See <a href="http://mongodb.github.io/mongo-java-driver/3.0/bson/codecs/">example</a>
 *
 * @author Eugene Voevodin
 * @author Sergii Kabashniuk
 * @see MongoDatabaseProvider
 */
@DynaModule
public class MongoCodecModule extends AbstractModule {

    @Override
    @SuppressWarnings("unchecked")
    protected void configure() {
        final Multibinder<CodecProvider> binder = Multibinder.newSetBinder(binder(), CodecProvider.class);
        binder.addBinding().toInstance(new CodecProvider() {
            @Override
            public <T> Codec<T> get(Class<T> clazz, CodecRegistry registry) {
                if (clazz == WorkspaceImpl.class) {
                    return (Codec<T>)new WorkspaceImplCodec(registry);
                } else if (clazz == StackImpl.class) {
                    return (Codec<T>)new StackImplCodec(registry);
                } else if (clazz == WorkerImpl.class) {
                    return (Codec<T>)new WorkerImplCodec(registry);
                } else if (clazz == RecipeImpl.class) {
                    return (Codec<T>)new RecipeImplCodec(registry);
                }
                return null;
            }
        });
    }
}
