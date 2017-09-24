package ru.vyarus.guice.persist.orient.db;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.persist.PersistService;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.tx.OTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vyarus.guice.persist.orient.db.data.DataInitializer;
import ru.vyarus.guice.persist.orient.db.pool.PoolManager;
import ru.vyarus.guice.persist.orient.db.scheme.CustomTypesInstaller;
import ru.vyarus.guice.persist.orient.db.scheme.SchemeInitializationException;
import ru.vyarus.guice.persist.orient.db.scheme.SchemeInitializer;
import ru.vyarus.guice.persist.orient.db.transaction.TxConfig;
import ru.vyarus.guice.persist.orient.db.transaction.template.TxAction;
import ru.vyarus.guice.persist.orient.db.transaction.template.TxTemplate;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Responsible for database lifecycle. Creates db if necessary and call schema and data initializers
 * for just opened database. Also, responsible for pools lifecycle.
 *
 * @author Vyacheslav Rusakov
 * @see ru.vyarus.guice.persist.orient.db.scheme.SchemeInitializer
 * @see ru.vyarus.guice.persist.orient.db.data.DataInitializer
 * @since 24.07.2014
 */
@Singleton
public class DatabaseManager implements PersistService {
    private final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);

    private final String uri;
    private final boolean autoCreate;

    private final List<PoolManager> pools;
    private final CustomTypesInstaller customTypesInstaller;
    private final SchemeInitializer modelInitializer;
    private final DataInitializer dataInitializer;
    private final TxTemplate txTemplate;

    // used to allow multiple start/stop calls (could be if service managed directly and PersistFilter registered)
    private boolean initialized;
    private Set<DbType> supportedTypes;

    @Inject
    public DatabaseManager(
            @Named("orient.uri") final String uri,
            @Named("orient.db.autocreate") final boolean autoCreate,
            final Set<PoolManager> pools,
            final CustomTypesInstaller customTypesInstaller,
            final SchemeInitializer modelInitializer,
            final DataInitializer dataInitializer,
            final TxTemplate txTemplate) {

        this.uri = Preconditions.checkNotNull(uri, "Database name required");
        this.autoCreate = autoCreate;
        this.pools = Lists.newArrayList(pools);
        this.customTypesInstaller = customTypesInstaller;
        this.modelInitializer = modelInitializer;
        this.dataInitializer = dataInitializer;
        this.txTemplate = txTemplate;
        // sort pools to correct startup order
        Collections.sort(this.pools, new Comparator<PoolManager>() {
            @Override
            public int compare(final PoolManager o1, final PoolManager o2) {
                return o1.getType().compareTo(o2.getType());
            }
        });
    }

    @Override
    public void start() {
        if (initialized) {
            logger.warn("Duplicate initialization prevented. Check your initialization logic: "
                    + "persistent service should not be started two or more times");
            return;
        }

        createIfRequired();
        startPools();
        logger.debug("Registered types: {}", supportedTypes);
        logger.debug("Initializing database: '{}'", uri);
        customTypesInstaller.install(uri);
        // no tx (because of schema update - orient requirement)
        try {
            txTemplate.doInTransaction(new TxConfig(OTransaction.TXTYPE.NOTX), new TxAction<Void>() {
                @Override
                public Void execute() throws Throwable {
                    modelInitializer.initialize();
                    return null;
                }
            });
        } catch (Throwable throwable) {
            throw new SchemeInitializationException("Failed to initialize scheme", throwable);
        }

        // db ready to work
        initialized = true;
        // tx may be enabled (it's up to implementer)
        dataInitializer.initializeData();
    }

    @Override
    public void stop() {
        if (!initialized) {
            // prevent double stop
            return;
        }
        initialized = false;
        stopPools();
    }

    /**
     * @return set of supported database types (according to registered pools)
     */
    public Set<DbType> getSupportedTypes() {
        return ImmutableSet.copyOf(supportedTypes);
    }

    /**
     * @param type db type to check
     * @return true if db tpe supported (supporting pool registered), false otherwise
     */
    public boolean isTypeSupported(final DbType type) {
        return supportedTypes.contains(type);
    }

    protected void createIfRequired() {
        // create if required (without creation work with db is impossible)
        // memory, local, plocal modes support simplified db creation,
        // but remote database must be created differently
        if (autoCreate && isLocalDatabase()) {
            final ODatabaseDocumentTx database = new ODatabaseDocumentTx(uri);
            try {
                if (!database.exists()) {
                    logger.info("Creating database: '{}'", uri);
                    database.create();
                }
            } finally {
                database.close();
            }
        }
    }

    /**
     * @return true if database is local, false for remote
     */
    private boolean isLocalDatabase() {
        return !this.uri.startsWith("remote:");
    }

    protected void startPools() {
        supportedTypes = Sets.newHashSet();
        for (PoolManager<?> pool : pools) {
            // if pool start failed, entire app start should fail (no catch here)
            pool.start(uri);
            supportedTypes.add(Preconditions.checkNotNull(pool.getType(),
                    "Pool %s doesn't declare correct pool type", pool.getClass().getSimpleName()));
        }
    }

    protected void stopPools() {
        for (PoolManager<?> pool : pools) {
            try {
                pool.stop();
            } catch (Throwable ex) {
                // continue to properly shutdown all pools
                logger.error("Pool '" + pool.getType() + "' shutdown failed (" + pool.getClass() + ")", ex);
            }
        }
    }
}
