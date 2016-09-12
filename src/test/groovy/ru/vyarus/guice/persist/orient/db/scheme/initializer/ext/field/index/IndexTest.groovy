package ru.vyarus.guice.persist.orient.db.scheme.initializer.ext.field.index

import com.orientechnologies.orient.core.metadata.schema.OClass
import com.orientechnologies.orient.core.metadata.schema.OType
import ru.vyarus.guice.persist.orient.db.scheme.SchemeInitializationException
import ru.vyarus.guice.persist.orient.db.scheme.initializer.ext.AbstractSchemeExtensionTest

/**
 * @author Vyacheslav Rusakov 
 * @since 09.03.2015
 */
class IndexTest extends AbstractSchemeExtensionTest {

    @Override
    String getModelPackage() {
        return "ru.vyarus.guice.persist.orient.db.scheme.initializer.ext.field.index"
    }

    def "Check index creation"() {

        when: "first class registration"
        schemeInitializer.register(IndexModel)
        def clazz = db.getMetadata().getSchema().getClass(IndexModel)
        then: "indexes created"
        clazz.getClassIndexes().size() == 3
        clazz.getClassIndex("IndexModel.foo").getType() == OClass.INDEX_TYPE.NOTUNIQUE.name()
        clazz.getClassIndex("customName").getType() == OClass.INDEX_TYPE.FULLTEXT.name()
        !clazz.getClassIndex("nulls").getDefinition().isNullValuesIgnored()

        when: "call for already registered indexes"
        // mark indexes
        clazz.getClassIndex("IndexModel.foo").getConfiguration().field("old", true)
        clazz.getClassIndex("customName").getConfiguration().field("old", true)
        clazz.getClassIndex("nulls").getConfiguration().field("old", true)
        schemeInitializer.clearModelCache()
        schemeInitializer.register(IndexModel)
        clazz = db.getMetadata().getSchema().getClass(IndexModel)
        then: "nothing changed"
        clazz.getClassIndexes().size() == 3
        clazz.getClassIndex("IndexModel.foo").getConfiguration().field("old")
        clazz.getClassIndex("customName").getConfiguration().field("old")
        clazz.getClassIndex("nulls").getConfiguration().field("old")
        clazz.getClassIndex("IndexModel.foo").getType() == OClass.INDEX_TYPE.NOTUNIQUE.name()
        clazz.getClassIndex("customName").getType() == OClass.INDEX_TYPE.FULLTEXT.name()
        !clazz.getClassIndex("nulls").getDefinition().isNullValuesIgnored()
    }

    def "Check index re-create"() {

        when: "index already exist with different type"
        def clazz = db.getMetadata().getSchema().createClass(IndexModel)
        clazz.createProperty("foo", OType.STRING)
        clazz.createProperty("bar", OType.STRING)
        clazz.createProperty("nulls", OType.STRING)
        clazz.createIndex('IndexModel.foo', OClass.INDEX_TYPE.DICTIONARY, "foo")
        clazz.createIndex('customName', OClass.INDEX_TYPE.DICTIONARY, "bar")
        clazz.createIndex('nulls', OClass.INDEX_TYPE.NOTUNIQUE, "nulls")
        // marking old index
        clazz.getClassIndex("IndexModel.foo").getConfiguration().field("old", true)
        clazz.getClassIndex("customName").getConfiguration().field("old", true)
        clazz.getClassIndex("nulls").getConfiguration().field("old", true)
        schemeInitializer.register(IndexModel)
        clazz = db.getMetadata().getSchema().getClass(IndexModel)
        then: "indexes re-created"
        clazz.getClassIndexes().size() == 3
        clazz.getClassIndex("IndexModel.foo").getConfiguration().field("old") == null
        clazz.getClassIndex("customName").getConfiguration().field("old") == null
        clazz.getClassIndex("nulls").getConfiguration().field("old") == null
        clazz.getClassIndex("IndexModel.foo").getType() == OClass.INDEX_TYPE.NOTUNIQUE.name()
        clazz.getClassIndex("customName").getType() == OClass.INDEX_TYPE.FULLTEXT.name()
        !clazz.getClassIndex("nulls").getDefinition().isNullValuesIgnored()
    }

    def "Check existing index with different fields"() {

        when: "index already exist with different fields"
        def clazz = db.getMetadata().getSchema().createClass(IndexModel)
        clazz.createProperty("foo", OType.STRING)
        clazz.createProperty("bar", OType.STRING)
        clazz.createIndex('IndexModel.foo', OClass.INDEX_TYPE.DICTIONARY, "bar")
        schemeInitializer.register(IndexModel)
        then: "error"
        thrown(SchemeInitializationException)
    }

    def "Check multiple indexes"() {

        when: "multiple indexes defined"
        schemeInitializer.register(MultipleIndexesModel)
        def clazz = db.getMetadata().getSchema().getClass(MultipleIndexesModel)
        then: "indexes created"
        clazz.getClassIndexes().size() == 2
        clazz.getClassIndex("test1").getType() == OClass.INDEX_TYPE.NOTUNIQUE.name()
        clazz.getClassIndex("test2").getType() == OClass.INDEX_TYPE.FULLTEXT.name()
    }
}