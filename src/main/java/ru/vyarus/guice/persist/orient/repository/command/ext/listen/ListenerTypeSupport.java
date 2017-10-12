package ru.vyarus.guice.persist.orient.repository.command.ext.listen;

import com.google.inject.Injector;
import com.orientechnologies.orient.core.command.OCommandRequest;
import com.orientechnologies.orient.core.command.OCommandResultListener;
import ru.vyarus.guice.persist.orient.repository.core.spi.parameter.ParamInfo;

/**
 * Listener type handler.
 *
 * @author Vyacheslav Rusakov
 * @since 29.09.2017
 */
public interface ListenerTypeSupport {

    /**
     * Check listener parameter correctness.
     *
     * @param query      query string
     * @param param      annotated listener parameter
     * @param returnType method return type
     */
    void checkParameter(String query, ParamInfo<Listen> param, Class<?> returnType);

    /**
     * Checks listener compatibility with command object and wraps listener if required.
     *
     * @param command       command object
     * @param listener      listener instance (passed in annotated parameter)
     * @param injector      injector instance
     * @param transactional true if listener execution must be wrapped with transaction
     * @return processed listener to apply to command
     */
    OCommandResultListener processListener(OCommandRequest command,
                                           Object listener,
                                           Injector injector,
                                           boolean transactional);
}
