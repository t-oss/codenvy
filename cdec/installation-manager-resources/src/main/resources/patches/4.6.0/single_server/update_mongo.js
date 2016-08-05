/* https://github.com/codenvy/codenvy/issues/432 */
var fixDevWorkspaces = function() {
    var organizationDb = db.getSiblingDB('organization');

    var found = organizationDb.workspaces2.find({"config.environments.machineConfigs.isDev": { $exists: true }}).count();
    var updated = 0;
    organizationDb.workspaces2.find({"config.environments.machineConfigs": { $exists: true }}).snapshot().forEach(
        function(doc) {
        doc.config.environments.forEach(
                function(env) {
                    env.machineConfigs.forEach(
                        function(config) {
                            if (config.isDev) {
                            config.dev = config.isDev;
                            delete config.isDev;
                                updated++;
                            }
                        }
                    )
                }
            )
            organizationDb.workspaces2.save(doc);
        }
    )

    print("Workspaces with 'isDev' field: " + found)
    print("Workspace configs updated from 'isDev' to 'dev': " + updated);
}

var fixDevStacks = function() {
    var organizationDb = db.getSiblingDB('organization');

    var found = organizationDb.stacks.find({"workspaceConfig.environments.machineConfigs.isDev": { $exists: true }}).count();
    var updated = 0;
    organizationDb.stacks.find({"workspaceConfig.environments.machineConfigs": { $exists: true }}).snapshot().forEach(
        function(doc) {
        doc.workspaceConfig.environments.forEach(
                function(env) {
                    env.machineConfigs.forEach(
                        function(config) {
                            if (config.isDev) {
                            config.dev = config.isDev;
                            delete config.isDev;
                                updated++;
                            }
                        }
                    )
                }
            )
            organizationDb.stacks.save(doc);
        }
    )

    print("Stacks: Stacks with 'isDev' field: " + found)
    print("Stacks: Workspace configs updated from 'isDev' to 'dev': " + updated);
}

var fixDevFactories = function() {
    var factoryDb = db.getSiblingDB('factory');

    var found = factoryDb.factory.find({"factory.workspace.environments.machineConfigs.isDev": { $exists: true }}).count();
    var updated = 0;
    factoryDb.factory.find({"factory.workspace.environments.machineConfigs": { $exists: true }}).snapshot().forEach(
        function(doc) {
        doc.factory.workspace.environments.forEach(
                function(env) {
                    env.machineConfigs.forEach(
                        function(config) {
                            if (config.isDev) {
                            config.dev = config.isDev;
                            delete config.isDev;
                                updated++;
                            }
                        }
                    )
                }
            )
            factoryDb.factory.save(doc);
        }
    )

    print("Factory: Factories with 'isDev' field: " + found)
    print("Factory: Workspace configs updated from 'isDev' to 'dev': " + updated);
}

fixDevWorkspaces();
fixDevStacks();
fixDevFactories();
