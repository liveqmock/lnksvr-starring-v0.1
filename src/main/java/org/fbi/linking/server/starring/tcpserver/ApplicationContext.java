package org.fbi.linking.server.starring.tcpserver;

import org.fbi.linking.processor.Processor;
import org.fbi.linking.processor.ProcessorContext;
import org.fbi.linking.processor.ProcessorException;

import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by zhanrui on 2013-5-29.
 */
public class ApplicationContext implements ProcessorContext {
    private Map<String,Object> attributes = new ConcurrentHashMap<String,Object>();
    private Map<String,String> parameters = new ConcurrentHashMap<String,String>();

    @Override
    public ProcessorContext getContext(String s) {
        return null;
    }

    @Override
    public String getContextPath() {
        return null;
    }

    @Override
    public Processor getProcessor(String s) throws ProcessorException {
        return null;
    }

    @Override
    public String getInitParameter(String name) {
        return parameters.get(name);
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return (Enumeration<String>)(parameters.keySet());
    }

    @Override
    public boolean setInitParameter(String name, String value) {
        if (parameters.containsKey(name)) {
            return false;
        }
        parameters.put(name, value);
        return true;
    }

    @Override
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return (Enumeration<String>)attributes.keySet();
    }

    @Override
    public void setAttribute(String name, Object value) {
        if (name == null)
            throw new IllegalArgumentException("Attribute name can not be empty.");

        if (value == null) {
            removeAttribute(name);
            return;
        }
        attributes.put(name, value);
    }

    @Override
    public void removeAttribute(String name) {
        boolean found = false;
        found = attributes.containsKey(name);
        if (found) {
            attributes.remove(name);
        }
    }
}
