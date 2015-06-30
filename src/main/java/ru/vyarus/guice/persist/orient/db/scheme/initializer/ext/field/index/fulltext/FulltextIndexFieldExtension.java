package ru.vyarus.guice.persist.orient.db.scheme.initializer.ext.field.index.fulltext;

import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.object.db.OObjectDatabaseTx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vyarus.guice.persist.orient.db.scheme.initializer.core.spi.SchemeDescriptor;
import ru.vyarus.guice.persist.orient.db.scheme.initializer.core.spi.field.FieldExtension;
import ru.vyarus.guice.persist.orient.db.scheme.initializer.ext.field.index.IndexValidationSupport;

import javax.inject.Singleton;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

/**
 * {@link FulltextIndex} scheme model field extension.
 *
 * @author Vyacheslav Rusakov
 * @since 14.06.2015
 */
@Singleton
public class FulltextIndexFieldExtension implements FieldExtension<FulltextIndex> {
    public static final String INDEX_RADIX = "indexRadix";
    public static final String IGNORE_CHARS = "ignoreChars";
    public static final String SEPARATOR_CHARS = "separatorChars";
    public static final String MIN_WORD_LENGTH = "minWordLength";
    public static final String STOP_WORDS = "stopWords";
    private final Logger logger = LoggerFactory.getLogger(FulltextIndexFieldExtension.class);

    @Override
    public void beforeRegistration(final OObjectDatabaseTx db, final SchemeDescriptor descriptor,
                                   final Field field, final FulltextIndex annotation) {
        // not needed
    }

    @Override
    public void afterRegistration(final OObjectDatabaseTx db, final SchemeDescriptor descriptor,
                                  final Field field, final FulltextIndex annotation) {
        final String property = field.getName();
        final String model = descriptor.schemeClass;
        final String name = Objects.firstNonNull(Strings.emptyToNull(annotation.name().trim()), model + '.' + property);
        final List<String> stopWords = Arrays.asList(annotation.stopWords());
        final OClass clazz = db.getMetadata().getSchema().getClass(model);
        final OIndex<?> classIndex = clazz.getClassIndex(name);
        final OClass.INDEX_TYPE type = annotation.useHashIndex()
                ? OClass.INDEX_TYPE.FULLTEXT_HASH_INDEX : OClass.INDEX_TYPE.FULLTEXT;
        if (!descriptor.initialRegistration && classIndex != null) {
            final IndexValidationSupport support = new IndexValidationSupport(classIndex, logger);

            support.checkTypeCompatible(OClass.INDEX_TYPE.FULLTEXT, OClass.INDEX_TYPE.FULLTEXT_HASH_INDEX);
            support.checkFieldsCompatible(property);

            final boolean correct = isIndexCorrect(support, type, annotation, stopWords);
            if (!correct) {
                support.dropIndex(db);
            } else {
                // index ok
                return;
            }
        }
        final ODocument metadata = createMetadata(annotation, stopWords);
        clazz.createIndex(name, type.name(), null, metadata, null, new String[]{property});
        logger.info("Fulltext index '{}' ({} [{}]) {} created", name, model, property, type);
    }

    private boolean isIndexCorrect(final IndexValidationSupport support, final OClass.INDEX_TYPE type,
                                   final FulltextIndex annotation, final List<String> stopWords) {
        final OIndex classIndex = support.getIndex();
        final ODocument metadata = classIndex.getConfiguration();
        return support
                .isIndexSigns(metadata.field(INDEX_RADIX),
                        metadata.field(IGNORE_CHARS),
                        metadata.field(SEPARATOR_CHARS),
                        metadata.field(MIN_WORD_LENGTH),
                        metadata.field(STOP_WORDS))
                .matchRequiredSigns(type, annotation.indexRadix(),
                        annotation.ignoreChars(),
                        annotation.separatorChars(),
                        annotation.minWordLength(),
                        stopWords);
    }

    private ODocument createMetadata(final FulltextIndex annotation, final List<String> stopWords) {
        final ODocument metadata = new ODocument();
        metadata.field(INDEX_RADIX, annotation.indexRadix());
        metadata.field(STOP_WORDS, stopWords);
        metadata.field(SEPARATOR_CHARS, annotation.separatorChars());
        metadata.field(IGNORE_CHARS, annotation.ignoreChars());
        metadata.field(MIN_WORD_LENGTH, annotation.minWordLength());
        return metadata;
    }
}