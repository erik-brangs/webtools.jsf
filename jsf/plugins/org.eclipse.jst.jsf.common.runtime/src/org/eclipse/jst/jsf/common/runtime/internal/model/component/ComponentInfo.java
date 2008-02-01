package org.eclipse.jst.jsf.common.runtime.internal.model.component;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.eclipse.jst.jsf.common.runtime.internal.model.ViewObject;
import org.eclipse.jst.jsf.common.runtime.internal.model.decorator.FacetDecorator;

/**
 * Models a basic UI component instance
 * 
 * TODO: should implement a visitor pattern to traverse component trees
 * 
 * @author cbateman
 * 
 */
public class ComponentInfo extends ViewObject implements Serializable
{
    /**
     * serializable id
     */
    private static final long serialVersionUID = 2517204356825585699L;

    private final static int DEFAULT_ARRAY_SIZE = 4;

    /**
     * the component id
     */
    protected final String _id;
    /**
     * the component's parent or null if none
     */
    protected final ComponentInfo _parent;
    /**
     * the type info for this component
     */
    protected final ComponentTypeInfo _componentTypeInfo;
    /**
     * the rendered flage
     */
    protected final boolean _isRendered;

    private transient BeanPropertyManager _beanPropertyManager = new BeanPropertyManager(
            this);

    // initialized
    // by
    // getBeanProperties

    /**
     * @param id
     * @param parent
     * @param componentTypeInfo
     * @param isRendered
     */
    protected ComponentInfo(final String id, final ComponentInfo parent,
            final ComponentTypeInfo componentTypeInfo, final boolean isRendered)
    {
        _id = translateForNull(id);
        _parent = parent;
        _componentTypeInfo = componentTypeInfo;
        _isRendered = isRendered;
    }

    /**
     * Construct a new component info using the attributes keyed by name in
     * attributes to set values. The names must match the corresponding bean
     * property names. Primitives should be wrapped in their corresponding
     * object types. Exceptions will be thrown if there is a type mismatch on an
     * expected type. Number will be used for all numeric primitive wrappers an
     * the corresponding "to" will be called.
     * 
     * @param parent
     * @param componentTypeInfo
     * @param attributes
     * @throws ClassCastException
     *             if an attribute's value doesn't match the expected type
     * @throws NullPointerException
     *             if an attribute value is null for a value whose type is
     *             expected to be primitive
     * @throws IllegalArgumentException
     *             if attributes does not contain a required key.
     */
    protected ComponentInfo(final ComponentInfo parent,
            final ComponentTypeInfo componentTypeInfo, final Map attributes)
    {
        this(getStringProperty("id", attributes, false), parent,
                componentTypeInfo, getBooleanProperty("rendered", attributes));
    }

    /**
     * @param key
     * @param attributes
     * @param mandatory
     * @return the value in attributes at location key, forcing a
     *         ClassCastException if it turns out not to be a String.
     * @throws ClassCastException
     *             if the attribute for key is not a String
     * @throws IllegalArgumentException
     *             if the attribute for key is null but mandatory is true.
     */
    protected static String getStringProperty(final String key,
            final Map attributes, final boolean mandatory)
    {
        final Object value = attributes.get(key);

        if (mandatory && value == null)
        {
            throw new IllegalArgumentException(key
                    + " is a mandatory attribute");
        }
        return (String) value;
    }

    /**
     * @param key
     * @param attributes
     * 
     * @return the value in attributes at location, forcing a ClassCastExceptio
     *         if it is no a Boolean.
     * @throws IllegalArgumentException
     *             if key is not found (all boolean attributes are mandatory
     *             since there is no valid state for unset.
     */
    protected static boolean getBooleanProperty(final String key,
            final Map attributes)
    {
        final Boolean value = (Boolean) attributes.get(key);

        if (value == null)
        {
            throw new IllegalArgumentException(key + "is mandatory");
        }

        return value.booleanValue();
    }

    /**
     * @param key
     * @param attributes
     * @return the integer property for key. Casts the value to Number and calls
     *         Number.intValue().
     */
    protected static int getIntegerProperty(final String key,
            final Map attributes)
    {
        final Number value = (Number) attributes.get(key);

        if (value == null)
        {
            throw new IllegalArgumentException(key + " is mandatory");
        }

        return value.intValue();
    }

    /**
     * @param key
     * @param attributes
     * @return the component info value from attributes
     */
    protected static ComponentInfo getComponentProperty(final String key,
            final Map attributes)
    {
        return (ComponentInfo) attributes.get(key);
    }

    private String translateForNull(final String arg)
    {

        if (arg == null || "!".equals(arg.trim()))
        {
            return null;
        }
        return arg.trim();
    }

    private List/* <ComponentInfo> */_children;

    /**
     * @return the id
     */
    public final String getId()
    {
        return _id;
    }

    /**
     * @return the component type info
     */
    public final ComponentTypeInfo getComponentTypeInfo()
    {
        return _componentTypeInfo;
    }

    /**
     * @return the children. List is unmodifiable. List contains all children
     *         including facets.
     */
    public final synchronized List/* <ComponentInfo> */getChildren()
    {
        if (_children == null)
        {
            return Collections.EMPTY_LIST;
        }
        return Collections.unmodifiableList(_children);
    }

    /**
     * Get the sub-set of {@link #getChildren()} that are facets. This is a
     * convenience method for {@link #getDecorators(Class)}
     * 
     * @return all component children that are facets
     */
    public final List getFacets()
    {
        return getDecorators(ComponentFactory.FACET);
    }

    /**
     * @param childComponent
     */
    public final synchronized void addChild(final ComponentInfo childComponent)
    {
        if (_children == null)
        {
            _children = new ArrayList(DEFAULT_ARRAY_SIZE);
        }
        _children.add(childComponent);
    }

    /**
     * @param name
     * @param facetComponent
     */
    public final synchronized void addFacet(final String name,
            final ComponentInfo facetComponent)
    {
        addChild(facetComponent);
        addDecorator(new FacetDecorator(name, facetComponent));
    }

    /**
     * @param component
     * @return if component corresponds to a facet of this component, returns
     *         the name of that facet. Returns null if not found.
     */
    public final String getFacetName(final ComponentInfo component)
    {
        if (component == null)
        {
            return null;
        }

        final List facets = getDecorators(ComponentFactory.FACET);

        for (final Iterator it = facets.iterator(); it.hasNext();)
        {
            final FacetDecorator facet = (FacetDecorator) it.next();
            if (component == facet.getDecorates())
            {
                return facet.getName();
            }
        }

        // component is not a facet
        return null;
    }

    /**
     * @param name
     * @return if this has a facet called name, then returns it's single root
     *         component.
     */
    public final synchronized ComponentInfo getFacet(final String name)
    {
        if (name == null)
        {
            return null;
        }

        final List facets = getDecorators(ComponentFactory.FACET);

        for (final Iterator it = facets.iterator(); it.hasNext();)
        {
            final FacetDecorator facet = (FacetDecorator) it.next();
            if (name.equals(facet.getName()))
            {
                return facet.getDecorates();
            }
        }

        // not found
        return null;
    }

    public String toString()
    {
        final String parentId = _parent != null ? _parent.getId() : "null";
        String toString = getMostSpecificComponentName() + ": id=" + _id
                + ", parentId: " + parentId + ", family="
                + _componentTypeInfo.getComponentFamily() + ", render="
                + _componentTypeInfo.getRenderFamily() + ", rendered="
                + _isRendered;

        // use bean introspection to dump child properties
        if (this.getClass() != ComponentInfo.class)
        {
            toString += dumpProperties();
        }

        return toString;
    }

    private String dumpProperties()
    {
        String properties = "";
        try
        {
            final BeanInfo beanInfo = Introspector.getBeanInfo(this.getClass(),
                    ComponentInfo.class);

            final PropertyDescriptor[] descriptors = beanInfo
                    .getPropertyDescriptors();
            for (int i = 0; i < descriptors.length; i++)
            {
                final PropertyDescriptor desc = descriptors[i];
                final String name = desc.getName();
                final Object valueObj = desc.getValue(name);
                final String value = valueObj != null ? valueObj.toString()
                        : "null";
                properties += ", " + name + "=" + value;
            }
        }
        catch (final IntrospectionException e)
        {
            return "Error introspecting bean: " + e.getLocalizedMessage();
        }

        return properties;
    }

    /**
     * @return used for toString. Clients should not use.
     */
    protected String getMostSpecificComponentName()
    {
        return "UIComponent";
    }

    /**
     * @return the parent of this component or null.
     */
    public final ComponentInfo getParent()
    {
        return _parent;
    }

    /**
     * @return the rendered flag
     */
    public final boolean isRendered()
    {
        return _isRendered;
    }

    public synchronized void addAdapter(final Class adapterType,
            final Object adapter)
    {
        super.addAdapter(adapterType, adapter);

        // force an update on the next call to getBeanProperties
        _beanPropertyManager.reset();
    }

    public synchronized Object removeAdapter(final Class adapterType)
    {
        final Object removed = super.removeAdapter(adapterType);

        _beanPropertyManager.reset();

        return removed;
    }

    /**
     * @return the set of all bean property names for this component. The set is
     *         unmodifiable and will throw exceptions if modification is
     *         attempted.
     */
    protected final Map/* <String, ComponentBeanProperty> */getBeanProperties()
    {
        return Collections.unmodifiableMap(_beanPropertyManager
                .getBeanProperties());
    }

    /**
     * This is similar to the runtime getAttributes().get(name) call. The reason
     * we don't implement a Map of all attribute values is that the implicit
     * property structure can change at any time due to add/removeAdapter. To
     * get all attributes known for a component, instead use:
     * 
     * The synchronized block is advised to protect against concurrent
     * modification exceptions on the keySet iterator.
     * 
     * @param name
     * 
     * @return the value of the attribute or null if none.
     * 
     */
    public synchronized ComponentBeanProperty getAttribute(final String name)
    {
        return (ComponentBeanProperty) getBeanProperties().get(name);
    }

    /**
     * @return the set of valid attribute names.  The Set is not modifiable.
     */
    public synchronized Set/*<String>*/  getAttributeNames()
    {
        return getBeanProperties().keySet();
    }
    /**
     * Stores a bean property descriptor along information about which
     * implementation class declares it and what key to pass to getAdapter() in
     * order to get it.
     * 
     */
    public final static class ComponentBeanProperty
    {
        private final PropertyDescriptor _propertyDescriptor;
        private final Object _declaringImplementation;
        private final Class _adapterKeyClass;

        // only instantiable locally
        private ComponentBeanProperty(Class adapterKeyClass,
                Object declaringImplementationClass,
                PropertyDescriptor propertyDescriptor)
        {
            super();
            _adapterKeyClass = adapterKeyClass;
            _declaringImplementation = declaringImplementationClass;
            _propertyDescriptor = propertyDescriptor;
        }

        /**
         * @return the value of property
         */
        public final Object getValue()
        {
            final Method method = _propertyDescriptor.getReadMethod();
            if (method != null)
            {
                try
                {
                    method.setAccessible(true);
                    return method.invoke(_declaringImplementation,
                            new Object[0]);
                }
                catch (IllegalArgumentException e)
                {
                    e.printStackTrace();
                }
                catch (IllegalAccessException e)
                {
                    e.printStackTrace();
                }
                catch (InvocationTargetException e)
                {
                    e.printStackTrace();
                }
            }
            // if any step fails, return null
            return null;
        }

        /**
         * @return the property descriptor
         */
        public final PropertyDescriptor getPropertyDescriptor()
        {
            return _propertyDescriptor;
        }

        /**
         * @return the implemenation
         */
        public final Object getDeclaringImplementationClass()
        {
            return _declaringImplementation;
        }

        /**
         * @return the adapter class for the interface that the declaring
         *         implementation is providing the impl for
         */
        public final Class getAdapterKeyClass()
        {
            return _adapterKeyClass;
        }
    }

    /**
     * Manages bean property information for a component
     * 
     * @author cbateman
     * 
     */
    protected final static class BeanPropertyManager
    {
        /**
         * a map of the bean property names exposed by this component including
         * all those added by addAdapter().
         * 
         * this is synthetic based the class definition and installed adapters
         * so as long that info is available, no need to serialize.
         */
        protected transient Map/* <String, ComponentBeanProperty> */_beanProperties; // lazily
        private final transient ComponentInfo _component;

        /**
         * @param component
         */
        protected BeanPropertyManager(final ComponentInfo component)
        {
            _component = component;
        }

        /**
         * Will throw exception of the calling thread already holds the "this"
         * monitor lock. This is to ensure that caller always acquires locks in
         * appropriate order to prevent deadlock.
         * 
         * @return the internal set of bean properties. This Set may be modified
         *         internally.
         */
        public Map getBeanProperties()
        {
            if (Thread.holdsLock(this))
            {
                throw new IllegalStateException(
                        "Must not already own this lock");
            }

            // must always acquire component lock first to prevent deadlock
            synchronized (_component)
            {
                synchronized (this)
                {
                    if (_beanProperties == null)
                    {
                        _beanProperties = calculateAllBeanPropNames(ViewObject.class);
                    }

                    return _beanProperties;
                }
            }
        }

        /**
         * Will throw exception if the calling thread already holds the "this"
         * monitor lock. This is to ensure that caller always acquires locks in
         * appropriate order to prevent deadlock.
         * 
         * Clears the internal map and sets to null. This will force it to be
         * completely new built on the next call to getBeanProperties
         */
        public void reset()
        {
            if (Thread.holdsLock(this))
            {
                throw new IllegalStateException(
                        "Must not already own this lock");
            }

            // must always acquire component lock first to prevent deadlock
            synchronized (_component)
            {
                synchronized (this)
                {
                    if (_beanProperties != null)
                    {
                        _beanProperties.clear();
                        _beanProperties = null;
                    }
                }
            }
        }

        /**
         * @param stopClass
         * @return a synchronized map of all bean property names on this class
         *         up to stopClass, as well as all adapter property names (as
         *         though this really implemented them).
         */
        private Map calculateAllBeanPropNames(final Class stopClass)
        {
            // use a set to prevents the duplicates
            final Map allProperties = new HashMap();

            {
                final Class myClass = _component.getClass();
                final List myProperties = getOrCreateBeanProperties(myClass,
                        stopClass);

                addToMap(myProperties, _component, myClass, allProperties);
            }

            {
                for (final Iterator it = _component.getAdapterMap().entrySet()
                        .iterator(); it.hasNext();)
                {
                    Map.Entry entry = (Entry) it.next();

                    final Class adapterClass = (Class) entry.getKey();
                    final Object declaringClass = entry.getValue();
                    // get all props, excluding the ones on Object.
                    final List props = getOrCreateBeanProperties(adapterClass,
                            null);
                    addToMap(props, declaringClass, adapterClass, allProperties);
                }
            }

            return Collections.synchronizedMap(allProperties);
        }

        private static void addToMap(
                final List/* <ComponentBeanProperty> */addThese,
                final Object declaringObject, final Class declaringAdapter,
                final Map toMe)
        {
            for (final Iterator it = addThese.iterator(); it.hasNext();)
            {
                final PropertyDescriptor desc = (PropertyDescriptor) it.next();

                if (!toMe.containsKey(desc.getName()))
                {
                    toMe.put(desc.getName(), new ComponentBeanProperty(
                            declaringAdapter, declaringObject, desc));
                }
                else
                {
                    // TODO: need logging
                    System.err
                            .println("Name collision in properties.  Trying to add ["
                                    + desc.toString()
                                    + " when already have "
                                    + toMe.get(desc.getName()));
                }
            }
        }

        /**
         * lazily loaded with the local properties (those not defined using
         * adapters)
         * 
         * MUST INITIALIZE early so can synchronize on it
         */
        private transient static Map/* <Class, List<PropertyDescriptor> */PROPERTY_MAP = new HashMap();

        /**
         * @param startClass
         * @param stopClass
         * @return a unmodifiable list of properties starting from startClass.
         *         stopClass is only used if an entry doesn't already exist in
         *         PROPERTY_MAP for startClass. The method is synchronized on
         *         the PROPERTY_MAP it updates.
         */
        protected static List/* <PropertyDescriptor */getOrCreateBeanProperties(
                final Class startClass, final Class stopClass)
        {
            synchronized (PROPERTY_MAP)
            {
                List localBeanProps = (List) PROPERTY_MAP.get(startClass);

                if (localBeanProps == null)
                {
                    localBeanProps = calculateBeanProperties(startClass,
                            stopClass);
                    PROPERTY_MAP.put(startClass, Collections
                            .unmodifiableList(localBeanProps));
                }

                return localBeanProps;
            }
        }

        /**
         * @param startClass
         * @param stopClass
         * @return a List<String> containing all of the bean names between
         *         startClass and stopClass. Start class must be a descendant
         *         (sub-class, sub-sub-class etc.) of stopClass. The properties
         *         on stopClass are excluded from analysis.
         */
        private static List/* <PropertyDescriptor> */calculateBeanProperties(
                final Class startClass, final Class stopClass)
        {
            BeanInfo beanInfo;
            List names = new ArrayList();

            try
            {
                beanInfo = Introspector.getBeanInfo(startClass, stopClass);
                final PropertyDescriptor[] descriptors = beanInfo
                        .getPropertyDescriptors();

                if (descriptors != null)
                {
                    names = Arrays.asList(descriptors);
                }
            }
            catch (final IntrospectionException e)
            {
                e.printStackTrace();
            }
            return names;
        }

    }
}