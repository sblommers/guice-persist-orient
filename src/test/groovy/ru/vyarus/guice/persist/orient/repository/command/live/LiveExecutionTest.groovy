package ru.vyarus.guice.persist.orient.repository.command.live

import com.orientechnologies.common.exception.OException
import com.orientechnologies.orient.core.command.OCommandResultListener
import com.orientechnologies.orient.core.db.record.ORecordOperation
import com.orientechnologies.orient.core.record.impl.ODocument
import com.orientechnologies.orient.core.sql.query.OLiveResultListener
import ru.vyarus.guice.persist.orient.AbstractTest
import ru.vyarus.guice.persist.orient.repository.RepositoryException
import ru.vyarus.guice.persist.orient.support.model.Model
import ru.vyarus.guice.persist.orient.support.modules.RepositoryTestModule
import spock.guice.UseModules

import javax.inject.Inject

/**
 * @author Vyacheslav Rusakov
 * @since 29.09.2017
 */
@UseModules(RepositoryTestModule)
class LiveExecutionTest extends AbstractTest {

    @Inject
    LiveCases repository

    def "Check live query"() {

        when: "subscribe query"
        Listener listener = new Listener()
        int token = repository.subscribe(listener)
        then: "subscribed"
        token != null
        listener.lastOp == null
        !listener.errored
        !listener.unsubscribed

        when: "insert event"
        Model saved = repository.save(new Model(name: "justnow"))
        sleep(70)
        then: "listener called"
        listener.lastOp != null
        listener.lastOp.type == ORecordOperation.CREATED
        listener.lastOp.record instanceof ODocument
        (listener.lastOp.record as ODocument).field("name") == "justnow"

        when: "remove event"
        repository.delete(saved)
        sleep(70)
        then: "listener called"
        listener.lastOp.type == ORecordOperation.DELETED
        listener.lastOp.record instanceof ODocument
        (listener.lastOp.record as ODocument).field("name") == "justnow"

        when: "unsubscribe"
        listener.lastOp = null
        repository.unsubscribe(token)
        sleep(70)
        then: "unsubscribed"
        listener.unsubscribed

        when: "new changes"
        repository.save(new Model(name: 'newchange'))
        then: "not received"
        listener.lastOp == null
    }

    def "Check error cases"() {
        Listener dummy = new Listener()

        when: "query without listener"
        repository.noListener()
        then: "error"
        thrown(RepositoryException)

        when: "not void method"
        repository.notInt(dummy)
        then: "error"
        thrown(RepositoryException)

        when: "bad listener"
        repository.badListener(new OCommandResultListener() {
            @Override
            boolean result(Object iRecord) {
                return false
            }

            @Override
            void end() {
            }

            @Override
            Object getResult() {
                return null
            }
        })
        then: "error"
        thrown(RepositoryException)

        when: "null listener"
        repository.subscribe(null)
        then: "error"
        thrown(RepositoryException)
    }

    static class Listener implements OLiveResultListener {

        ORecordOperation lastOp
        boolean unsubscribed
        boolean errored

        @Override
        void onLiveResult(int iLiveToken, ORecordOperation iOp) throws OException {
            lastOp = iOp
        }

        @Override
        void onError(int iLiveToken) {
            errored = true
        }

        @Override
        void onUnsubscribe(int iLiveToken) {
            unsubscribed = true
        }
    }
}
