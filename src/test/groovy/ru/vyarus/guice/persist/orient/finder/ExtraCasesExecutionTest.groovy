package ru.vyarus.guice.persist.orient.finder

import com.google.inject.Inject
import ru.vyarus.guice.persist.orient.AbstractTest
import ru.vyarus.guice.persist.orient.db.transaction.template.SpecificTxAction
import ru.vyarus.guice.persist.orient.db.transaction.template.TxAction
import ru.vyarus.guice.persist.orient.db.transaction.template.TxTemplate
import ru.vyarus.guice.persist.orient.support.finder.ExtraCasesFinder
import ru.vyarus.guice.persist.orient.support.model.Model
import ru.vyarus.guice.persist.orient.support.modules.FinderTestModule
import spock.guice.UseModules

/**
 * @author Vyacheslav Rusakov 
 * @since 05.08.2014
 */
@UseModules(FinderTestModule)
class ExtraCasesExecutionTest extends AbstractTest {

    @Inject
    ExtraCasesFinder finder
    @Inject
    TxTemplate simpleTemplate;

    def "Check cases"() {

        template.doInTransaction({ db ->
            db.save(new Model(name: 'John', nick: 'Doe'))
        } as SpecificTxAction)

        when: "object select for iterable"
        def res = finder.selectAll();
        then: "returned iterable"
        res.iterator().next() != null

        when: "object select for iterator"
        res = finder.selectAllIterator();
        then: "returned iterator"
        res.next() != null

        when: "graph select for iterator"
        res = simpleTemplate.doInTransaction(new TxAction() {
            @Override
            Object execute() throws Throwable {
                finder.selectAllVertex().next();
            }
        })
        then: "returned iterator"
        res != null

        when: "graph select for iterable"
        res = simpleTemplate.doInTransaction(new TxAction() {
            @Override
            Object execute() throws Throwable {
                finder.selectAllVertexIterable().iterator().next();
            }
        })
        then: "returned iterable"
        res != null

        when: "object select with set conversion"
        res = finder.selectAllAsSet();
        then: "returned set"
        res instanceof Set
        res.size() == 1

        when: "graph select with set conversion"
        res = finder.selectAllAsSetGraph();
        then: "returned set"
        res instanceof Set
        res.size() == 1

        when: "vararg check"
        res = finder.findWithVararg('Sam', 'Dan', 'John');
        then: "returned list"
        res.size() == 1

        when: "document connection overridden in select"
        res = finder.documentOverride();
        then: "returned list"
        res.size() == 1

//        when: "jdk8 optional"
//        res = finder.findJdkOptional();
//        then: "returned jdk optional"
//        res instanceof Optional
//        res.get()

        when: "guava optional"
        res = finder.findGuavaOptional();
        then: "returned guava optional"
        res instanceof com.google.common.base.Optional
        res.get()

        when: "converting empty collection to single element"
        res = finder.finderEmptyCollection();
        then: "empty optional returned"
        res instanceof com.google.common.base.Optional
        !res.isPresent()
    }
}