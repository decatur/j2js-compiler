package com.j2js.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.logging.Level;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import java.rmi.activation.ActivationException;

/**
  Class XmlBeanMapper offers a generic way to instanciate and initialize a JavaBean
  from an Xml document.

  <h2>Document Object Model</h2>
  The Xml document corresponds to a hierachy of values. A value is
  either a JavaBean or a primitive type. Each value except the root value
  is contained by its parent value.

  The document root corresponds to the JavaBean.
  Now we recursively apply the following rules:

  <ol>
  <li>An element or an attribute corresponds to a value if the enclosing value is a JavaBean
  which has a setter of the same name. The value is created and passed to the setter.
  An attribute can only be of primitive type.
  </li>
  <li>An element corresponds to a list of values if the enclosing value
  is a JavaBean and has an adder of the same name in singular.
  </li>
  <li>An element corresponds to a value if the enclosing value is a list.
  The value is created and passed to the adder of the enclosing value.
  </li>
  <li>The root element corresponds to the requested JavaBean.
  The name of the root element is not relevant but should coincide with
  the classname of the JavaBean.
  </li>
  </ol>

  <h3>Example</h3>

  <pre>
  &lt;person age="35">
    &lt;name>Scott Miller&lt;/name>
    &lt;addresses>
      &lt;address>
        &lt;street>Augusta Ave 7&lt;/street>
      &lt;/address>
      &lt;address>
        &lt;street>Chestnut Grove 53&lt;/street>
      &lt;/address>
    &lt;/addresses>
  &lt;/person>

  public class Person {
    public void setName(String name) {}
    public void setAge(int age) {}
    public void addAddress(Address address) {}
  }

  public class Address {
    public void setStreet(String street) {};
  }
  </pre>

  It is up to the user of Class XmlBeanMapper to provide a proper implementation
  for the mapped classes.

  <h3>Supported Primitive Types</h3>
  Supported primitive types are boolean, int, long, double and String.
  <h3>Furthermore the following datatypes are supported</h3>
  <ul>
  <li>Dates (java.sql.Date and java.util.Date)</li>
  <li>array of long, double, int and string</li>
  <li>Double, Long, Integer</li>
  </ul>
  <p>
  Valid values are
  the same as the valid arguments to the one-parameter constructors
  Boolean(String), Integer(String), Long(String), Double(String) and String(String)
  </p>

  <h3>Advanced Usage</h3>

  By default, the parameter of the setter or adder determines the class of
  the newly instanciated value. Of course, this will not work if the value
  has to be a subclass or an implementing class to an interfaces.
  <p>
  In these cases, the target class must be indicated by the <tt>metaInfClass</tt>
  attribute of the corresponding element, for example

  <pre>
  &lt;address country="Germany" metaInfClass="OffshoreAddress">
    &lt;street>Berliner Platz 11&lt;/street>
  &lt;/address>

  public class OffshoreAddress extends Address {
    public void setCountry(String country) {};
  }
  </pre>
  </p>

  <pre>
  &lt;metaInf>
    &lt;types>
      &lt;type elementName="name of element" className="full name of class"/>
      ...
    &lt;/types>
  &lt;/metaInf>
  </pre>

  <h3>Tips and Tricks</h3>
  Furthermore it is possible to use the valuestack for instructing the beanmapper to use
  some predefined values (setValueStack).
  If you use <pre>id="pop#5"</pre> or
  <pre><id pop="5"/></pre> the beanmapper will use the specified entry (in the example the fifth) of the
  given hashmap (setValueStack()) for initializing the corresponding field or attribute.
  Reference to constructor
  Url trick with recursive mapping; Multiple mapping.
  TODO: Complete documentation
  
  @deprecated

 */
public class XmlBeanMapper {

    /**
     * Structure to hold a key-value pair.
     */
    private class BeanInfo {
        Object bean;
        String key;

        /**
         * Constructor for the BeanInfo-Class.
         * @param newKey the key
         * @param newBean the bean
         */
        BeanInfo(String newKey, Object newBean) {
            key = newKey;
            bean = newBean;
        }
    }

    public class MetaInf {

        public class Type {
            String elementName;    // Element name.
            String className;

            public Type() {
                super();
            }

            public void setElement(String newElementName) {
                elementName = newElementName;
            }
            public void setClass(String newClassName) {
                className = newClassName;
            }
        }

        private HashMap<String, String> types = new HashMap<String, String>();

        public MetaInf() {
            super();
        }

        public void addType(Type type) {
            log(Level.FINEST, "Mapping element " + type.elementName + " to " + type.className);
            types.put(type.elementName, type.className);
        }

        public String getType(String elementName) {
            return types.get(elementName);
        }

        public java.util.Map getTypes() {
            return types;
        }
    }

    private static final int MAX_LINE_LENGTH = 76;

    private HashMap valueStack = new HashMap();
    private Stack<Object> stack = new Stack<Object>();
    private boolean ignore = false;
    private boolean trim = true;
    private MetaInf metaInf;
    private java.util.logging.Logger logger = null;
    private java.io.PrintStream printStream = null;
    private Level logLevel;
    private SimpleDateFormat dateFormatter = new SimpleDateFormat("dd.MM.yyyy HH:mm");

    /**
     * Creates a new instance of XmlBeanMapper.
     * 
     * @deprecated
     */
    public XmlBeanMapper() {
        super();
    }

    /**
     * Allows to ignore values that cannot be matched. Default is <tt>false</tt>.
     */
    public void setIgnoreMismatches(boolean theIgnore) {
        ignore = theIgnore;
    }

    /**
     * If true, all String-typed values will have their leading and trailing whitespace omitted.
     * Default is <tt>true</tt>.
     */
    public void setTrimWhitespace(boolean b) {
        trim = b;
    }

    /**
     * Sets the dateFormat used to parse the date values in the xml file.
     * @param dateFormat the date format. Default is "dd.MM.yyyy HH:mm".
     */
    public void setDateFormat(String dateFormat) {
        dateFormatter = new SimpleDateFormat(dateFormat);
    }

    /**
     * Sets a specified stream for logging at a specified logging level.
     */
    public void setLogStream(PrintStream newPrintStream, Level newLogLevel) {
        printStream = newPrintStream;
        logLevel = newLogLevel;
    }

    /**
     * Sets a specified stream for logging at a specified logging level.
     */
    public void setLogFile(java.io.File file, Level level) throws IOException {
        setLogStream(new java.io.PrintStream(new java.io.FileOutputStream(file)), level);
    }

    /**
     * Sets a specified logger for logging.
     */
    public void setLogger(java.util.logging.Logger theLogger) {
        logger = theLogger;
    }

    public void setMetaInf(MetaInf theMetaInf) {
        metaInf = theMetaInf;
    }

    public MetaInf getMetaInf() {
        return metaInf;
    }

    /**
     * See {@link #createBeanFromString(Class, String, Object) createBeanFromString}
     */
    public Object createBeanFromString(Class clazz, String xml) 
    throws IOException, ActivationException {
        return createBeanFromString(clazz, xml, null);
    }

    /**
     * Instanciate a bean for a given class from an Xml-string using a reference.
     * If you already have an instance try loadBean.
     *
     * @param clazz the class to instanciate from
     * @param xml the Xml-string
     * @param refObject the reference object
     * @return the created bean
     */
    public Object createBeanFromString(Class clazz, String xml, Object refObject)
            throws IOException, ActivationException {
        // Log only about MAX_LINE_LENGTH characters wide.
        String xmlString = xml;
        if (xml.length() > MAX_LINE_LENGTH) {
            xml.substring(0, MAX_LINE_LENGTH);
        }
        log(Level.INFO, "Creating " + clazz + " from string\n" + xmlString + " ...");
        return createBean(refObject, getDocumentElement(new java.io.ByteArrayInputStream(xml.getBytes())),
                clazz).bean;
    }

    public void loadBeanFromString(Object bean, String xml) 
                    throws IOException, ActivationException {
        Class clazz;
        if (bean instanceof Class) {
            clazz = (Class) bean;
            bean = null;
        } else {
            clazz = bean.getClass();
        }
        // Log only about 76 characters wide if string is longer than 76 chars
        // otherwise log the whole string
        int length = 0;
        if (xml.length() >= MAX_LINE_LENGTH) {
            length = MAX_LINE_LENGTH;
        } else {
            length = xml.length();
        }
        log(Level.INFO, "Loading" + clazz
                + " from string\n" + xml.substring(0, length)
                + " ...");
        Element root = getDocumentElement(new java.io.ByteArrayInputStream(xml.getBytes()));
        loadBean(clazz, bean, root);
    }

    /**
     * See {@link #createBeanFromFile(Class, String, Object) createBeanFromFile}
     */
    public Object createBeanFromFile(Class clazz, String path)
                throws IOException, ActivationException {
        return createBeanFromFile(clazz, path, null);
    }

    public Object createBeanFromFile(String path) 
                throws IOException, ActivationException {
        return createBeanFromFile(null, path, null);
    }

    /**
     * Instanciate a bean for a given class from an Xml-file using a reference.
     *
     * @param clazz the class to instanciate from
     * @param path the path to the Xml-file.
     * @param refObject the reference object
     */
    public Object createBeanFromFile(Class clazz, String path, Object refObject)
                throws IOException, ActivationException {
        Node document = getDocumentElement(new FileInputStream(path));
        if (clazz == null) {
            String className = getMetaInfClass(document);
            if (className == null) throw new ActivationException("Need to specify a class name with attribute 'metaInfClass'");
            try {
                clazz = Thread.currentThread().getContextClassLoader().loadClass(className);
            } catch (ClassNotFoundException e) {
                throw new ActivationException("Class " + className + " not found");
            }
        }
        log(Level.INFO, "Creating " + clazz + " from file " + path);
        BeanInfo beanInfo = createBean(refObject, document, clazz);
        return beanInfo.bean;
    }

    /**
     * Load a file into a bean.
     *
     * @param bean the bean to load the file into.
     * @param path the path of the file.
     */
    public void loadBean(Object bean, String path) throws IOException, ActivationException {
        Class clazz;
        if (bean instanceof Class) {
            clazz = (Class) bean;
            bean = null;
        } else {
            clazz = bean.getClass();
        }
        log(Level.INFO, "Loading " + clazz + " from file " + path);
        Element root = getDocumentElement(new FileInputStream(path));
        loadBean(clazz, bean, root);
    }

    /**
     * Load a file into a bean.
     *
     * @param bean the bean to load the file into.
     * @param stream an input stream
     */
    public void loadBean(Object bean, java.io.InputStream stream) throws IOException, ActivationException {
        Class clazz;
        if (bean instanceof Class) {
            clazz = (Class) bean;
            bean = null;
        } else {
            clazz = bean.getClass();
        }
        Element root = getDocumentElement(stream);
        loadBean(clazz, bean, root);
    }

    /**
     * Load a file into a bean.
     *
     * @param bean the bean to load the file into.
     * @param root the path of the file. 
     */
    public void loadBean(Object bean, Element root) throws ActivationException {
        Class clazz;
        if (bean instanceof Class) {
            clazz = (Class) bean;
            bean = null;
        } else {
            clazz = bean.getClass();
        }
        log(Level.INFO, "Loading " + clazz + " from Dom element");
        loadBean(clazz, bean, root);
    }

    /**
     * Utility to find a setter for a given set of methods. If a Class is specified,
     * then the setter will have an argument of that class.
     *
     * @param methods the methods
     * @param name the name of the setter
     * @param clazz the argument of the setter
     *
     * @return the index of the setter, or -1 if no suitable setter was found.
     */
    public int getSetter(Method[] methods, String name, Class<?> clazz) {
        //name = name.substring(0,1).toUpperCase() + name.substring(1);

        name = name.toLowerCase();
        String setter = "set" + name;
        int i = 0;
        int stringSetter = -1;
        int anySetter = -1;
        
        for (i = 0; i < methods.length; i++) {
            String methodName = methods[i].getName().toLowerCase();
            if (!methodName.equals(setter)) continue;
            
            Class<?>[] parameters = methods[i].getParameterTypes();
            if (parameters.length == 1) {
                if (clazz != null && parameters[0].isAssignableFrom(clazz)) {
                    return i;
                }
                if (parameters[0].equals(String.class)) {
                    stringSetter = i;
                } else {
                    anySetter = i;
                }
            } else if (parameters.length == 2 && parameters[0].equals(String.class)) {
                // key-value setter
                if (clazz != null && parameters[1].isAssignableFrom(clazz)) {
                    return i;
                }
                if (parameters[1].equals(String.class)) {
                    stringSetter = i;
                } else {
                    anySetter = i;
                }
            }
        }
        if (stringSetter != -1) return stringSetter;
        return anySetter;
    }

    /**
     * Utility to find an adder for a given set of methods.
     *
     * @param methods the methods
     * @param name the name of the adder
     *
     * @return the index of the adder, or -1 if no suitable adder was found.
     */
    public int getAdder(Method[] methods, String name, Class clazz) {
        name = name.toLowerCase();
        String adder1;
        String adder2;
        /*
         * If the name is a plural, then form a singular. For example, map
         * DataSources to DataSource or Entries to Entry.
         */
        if (name.endsWith("ies")) {
            adder1 = name.substring(0, name.length() - 3) + "y";
            adder2 = "";
        } else {
            adder1 = name.substring(0, name.length() - 1);
            adder2 = name.substring(0, name.length() - 2);
        }
        int i = 0;
        for (i = 0; i < methods.length; i++) {
            String methodName = methods[i].getName().toLowerCase();
            if (clazz != null) {
                Class<?>[] parameters = methods[i].getParameterTypes();
                if (parameters.length != 1 || !parameters[0].isAssignableFrom(clazz)) {
                    continue;
                }
            }
            if (methodName.startsWith("add")) {
                String s = methodName.substring(3, methodName.length());
                if (s.equals(adder1) || s.equals(adder2)) {
                    return i;
                }
            } else if (methodName.equals(adder1) || methodName.equals(adder2)) {
                return i;
            }
        }
        return -1;
    }

    protected void log(Level level, String msg) {
        if (logger != null) {
            logger.log(level, msg);
        }
        if (printStream != null && level.intValue() >= logLevel.intValue()) {
            printStream.println(msg);
        }
    }

    private Element getDocumentElement(java.io.InputStream stream) throws IOException {
        DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();
        dfactory.setNamespaceAware(true);

        BufferedInputStream bStream = new BufferedInputStream(stream);

        Document document = null;
        try {
            document = dfactory.newDocumentBuilder().parse(bStream);
        } catch (javax.xml.parsers.ParserConfigurationException e) {
            throw new IOException(e.getMessage());
        } catch (org.xml.sax.SAXParseException e) {
            throw new IOException(e.getMessage() + " in (line/column): ("
                + e.getLineNumber() + "/" + e.getColumnNumber() + ")");
        } catch (org.xml.sax.SAXException e) {
            throw new IOException(e.getMessage());
        }

        return document.getDocumentElement();
    }

    private String getMetaInfClass(Node node) {
        if (node.getNodeType() != Node.ELEMENT_NODE) {
            return null;
        }
        Node n = node.getAttributes().getNamedItem("metaInfClass");
        if (n == null) {
            return null;
        }
        return n.getNodeValue();
    }

    /**
     * Load an element node into a bean.
     *
     * @param bean the JavaBean to load into.
     * @param element the element to load.
     */
    private String loadBean(Class clazz, Object bean, Element element) throws ActivationException {
        //Class clazz = bean.getClass();
        Method[] methods = clazz.getMethods();
        String key = null;

        // Load all child-elements.
        Element child = getFirstElementChild(element);
        while (child != null) {
            String name = child.getNodeName();
            int index;

            if (name.equals("metaInf")) {
                /* metaInf elements are loaded into this XmlBeanMapper, not
                 * into the specified bean.
                 */
               MetaInf mi = new MetaInf();
               loadBean(mi.getClass(), mi, child);
               setMetaInf(mi);
            } else {
                Class c = null;

                String className = getMetaInfClass(child);
                if (className != null) {
                    try {
                        c = Thread.currentThread().getContextClassLoader().loadClass(className);
                    } catch (ClassNotFoundException e) {
                        throw new ActivationException("Class " + className + " not found");
                    }
                }

                if ((index = getSetter(methods, name, c)) >= 0) {
                    /* Register single node. */
                    loadProperty(methods[index], bean, child);
                } else if ((index = getAdder(methods, name, c)) >= 0) {
                    /* Register list of nodes. */
                    Element elem = getFirstElementChild(child);
                    while (elem != null) {
                        loadProperty(methods[index], bean, elem);
                        elem = getNextElement(elem);
                    }
                } else {
                    if (!ignore) {
                        throw new ActivationException("Could not match element " + name + "("
                                + clazz.getName() + ")");
                    }
                    log(Level.WARNING, "Ignoring " + name);
                }
            }

            child = getNextElement(child);
        }

        // Load all attributes.
        NamedNodeMap map = element.getAttributes();
        for (int i = 0; i < map.getLength(); i++) {
            Node node = map.item(i);
            String name = node.getNodeName();        
            if (name.equals("metaInfClass")) {
                continue;
            }
            if (name.equals("metaInfKey")) {
                key = node.getNodeValue();
                continue;
            }
            int index = getSetter(methods, name, null);
            if (index >= 0) {
                loadProperty(methods[index], bean, node);
            } else {
                if (!ignore) {
                    throw new ActivationException(
                            "Could not match attribute " + name + " for " + clazz);
                }
                log(Level.WARNING, "Ignoring " + name);
            }
        }
        return key;
    }

    /**
     * Instantiates an object for a given class using a single-argument constructor.
     * The argument type of the constructor must match the type of at least one
     * reference object on the stack. The top-most references on the stack is then
     * passed to the constructor.
     * <p>
     * <b>Warning</b>: If there is more than one suitable constructor, it is undefined which one of
     * these constructors are called.
     *
     * @return the instantiated object or null, if no suitable constructor was found
     * @throws ActivationException if a suitable constructor could not be invoked
     */
    private Object createBean(Class clazz) throws ActivationException {
        Constructor[] constructors = clazz.getConstructors();
        // Loop over all constructors with single argument.
        for (int i=0; i<constructors.length; i++) {
            Class[] types = constructors[i].getParameterTypes();
            if (types.length != 1) { // No single argument.
                continue;
            }
       
            log(Level.FINE, "Trying constructor with argument " + types[0]);

            // Loop over all references in the stack.
            for (int j = stack.size() - 1; j >= 0; j--) {
                Object ref = stack.get(j);
                if (ref != null && types[0].equals(ref.getClass())) {
                    // Found the specific constructor.
                    try {
                        return constructors[i].newInstance(new Object[]{ref});
                    } catch (Exception e) {
                        // InstantiationException or IllegalAccessException
                        throw new ActivationException("Could not instantiate " + clazz 
                                + " using constructor with argument " + ref.getClass());
                    }
                }
            }
        }
        return null;
    }

    private String checkPrimitiveType(Node node) throws ActivationException {
        if (node.getNodeType() != Node.ELEMENT_NODE) {
            return null;
        }
        NamedNodeMap map = node.getAttributes();
        if (map.getLength() == 0) {
            return null;
        }
        Node attribute = map.item(0);
        if (!ignore
                && !attribute.getNodeName().equals("pop")
                && !attribute.getNodeName().equals("metaInfKey")) {
            throw new ActivationException(
                    "Could not match " + attribute.getNodeName()
                    + " of primitive type or Class or String "
                    + node.getNodeName() + " has attribute ");
        }
        return attribute.getNodeValue();
    }

    /**
     * Instanciates a bean for a given class from a node element using a reference object.
     * If the class has a one-parameter constructor of the same type as the reference object,
     * then the reference object is passed to that constructor. If the reference object is null,
     * or if no suitable one-parameter constructor excists, then the default constructor
     * is used.
     * Important: java.util.Date will experience special treatment.
     *
     * @param refObject the reference object (may be null)
     * @param node the node element
     * @param clazz the class of the bean
     * @return the created bean
     */
    private BeanInfo createBean(Object refObject, Node node, Class clazz) throws ActivationException {
        stack.push(refObject);

        Object bean = null;
        String key = null;

        String nodeValue = "";        
        NamedNodeMap map = node.getAttributes();
        if (map != null && map.getLength() != 0) {
            Node attribute = map.item(0);
            if (attribute.getNodeName().equals("pop")) {
                nodeValue = (String) valueStack.get(attribute.getNodeValue()); 
            } else {
                nodeValue = getNodeValue(node);
            }
        } else {
            nodeValue = getNodeValue(node);
            if (nodeValue.indexOf("pop#") >= 0) {
                nodeValue = nodeValue.substring(nodeValue.indexOf("pop#") + 4);
                nodeValue = (String) valueStack.get(nodeValue);
            }
        }

        String className = clazz.toString();
        if (clazz.equals(String.class)) {
            key = checkPrimitiveType(node);
            bean = nodeValue;
        } else if (clazz.equals(Class.class)) {
            try {
                key = checkPrimitiveType(node);
                bean = Thread.currentThread().getContextClassLoader().loadClass(nodeValue);
            } catch (ClassNotFoundException e) {
                throw new ActivationException("Class " + nodeValue + " not found");
            }
        } else if (!className.startsWith("class") && !className.startsWith("interface")) {
            // Primitive type.
            key = checkPrimitiveType(node);

            if (clazz.equals(String.class)) {
                bean = nodeValue;
            } else if (className.equals("byte")) {
                bean = new Byte(nodeValue);
            } else if (className.equals("char")) {
                String s = nodeValue;
                if (s.length() != 1) {
                    throw new ActivationException("Character expected, but found '" + s + "'");
                }
                bean = new Character(s.charAt(0));
            } else if (className.equals("double")) {
                bean = new Double(nodeValue);
            } else if (className.equals("float")) {
                bean = new Float(nodeValue);
            } else if (className.equals("int")) {
                bean = new Integer(nodeValue);
            }  else if (className.equals("long")) {
                bean = new Long(nodeValue);
            } else if (className.equals("short")) {
                bean = new Short(nodeValue);
            } else if (className.equals("boolean")) {
                bean = new Boolean(nodeValue);
            }
        // arrays:
        // long:class [J
        // double:class [D
        // int:class [I
        // String:class [Ljava.lang.String;
        } else if (className.equals("class [J")) {
            StringTokenizer tokenizer = new StringTokenizer(nodeValue, ",");
            long[] array = new long[tokenizer.countTokens()];
            for (int i = 0; tokenizer.countTokens() > 0; i++) {
                array[i] = Long.parseLong(tokenizer.nextToken());
            }
            bean = array;
        } else if (className.equals("class [D")) {
            StringTokenizer tokenizer = new StringTokenizer(nodeValue, ",");
            double[] array = new double[tokenizer.countTokens()];
            for (int i = 0; tokenizer.countTokens() > 0; i++) {
                array[i] = Double.parseDouble(tokenizer.nextToken());
            }
            bean = array;
        } else if (className.equals("class [I")) {
            StringTokenizer tokenizer = new StringTokenizer(nodeValue, ",");
            int[] array = new int[tokenizer.countTokens()];
            for (int i = 0; tokenizer.countTokens() > 0; i++) {
                array[i] = Integer.parseInt(tokenizer.nextToken());
            }
            bean = array;
        } else if (className.equals("class [Ljava.lang.String;")) {
            StringTokenizer tokenizer = new StringTokenizer(nodeValue, ",");
            String[] array = new String[tokenizer.countTokens()];
            for (int i = 0; tokenizer.countTokens() > 0; i++) {
                array[i] = tokenizer.nextToken();
            }
            bean = array;
        } else if (className.equals("class java.lang.Double")) {
            bean = new Double(nodeValue);
        } else if (className.equals("class java.lang.Boolean")) {
            bean = new Boolean(nodeValue);
        } else if (className.equals("class java.lang.Long")) {
            bean = new Long(nodeValue);
        } else if (className.equals("class java.lang.Integer")) {
            bean = new Integer(nodeValue);
        } else if (className.indexOf("java.util.Date") > 0) {
            // we have a date => some kind of primitive type
            // special treating because of all useful methods being deprecated
            try {
                bean = dateFormatter.parse(nodeValue);
            } catch (ParseException e) {
                throw new ActivationException(
                    "Problems parsing a date field.", e);
            }
        } else if (className.indexOf("java.sql.Date") > 0) {
            // we have a date => some kind of primitive type
            // special treating because of all useful methods being deprecated
            try {
                bean = new java.sql.Date(dateFormatter.parse(nodeValue).getTime());
            } catch (ParseException e) {
                throw new ActivationException(
                    "Problems parsing a date field.", e);
            }
        } else {
            int modifiers = clazz.getModifiers();
            if ((modifiers & Modifier.ABSTRACT) != 0) { // Interface or abstract class.
                throw new ActivationException(
                    "Cannot instantiate " + Modifier.toString(modifiers) + " " + clazz.getName());
            }

            if (stack.size() > 0) {
                /**
                 * We have to instantiate the bean. Note that newInstance() will not do when
                 *   a) the class is an inner class,
                 *   b) a non-default constructor should be used.
                 * In these cases we have to find the specific constructor which has a 
                 * single argument of the same type as the parent bean.
                 */
                bean = createBean(clazz);
            }

            if (bean == null) {
                try {
                    bean = clazz.newInstance();
                } catch (Exception e) {
                    // InstantiationException or IllegalAccessException
                    throw new ActivationException("Could not instantiate " + clazz
                            + " using default constructor");
                }
            }

            key = loadBean(bean.getClass(), bean, (Element) node);
        }
        stack.pop();
        return new BeanInfo(key, bean);
    }

    /**
     * Load a node into a JavaBean and register it with a parent bean using
     * the specified method. If the method does not allow arguments, no node is
     * loaded. This corresponds to executing an action.
     *
     * @param method the setter or adder
     * @param obj the parent bean
     * @param the node to load
     */
    private void loadProperty(Method method, Object obj, Node node) throws ActivationException {
        Object[] args = null;
        Class[] types = method.getParameterTypes();
        Class type = null;

        if (types.length==1 || types.length==2) {
            args = new Object[types.length];

            String className = getMetaInfClass(node);
            if (className==null && metaInf!=null && metaInf.getType(node.getNodeName())!=null) {
                className = metaInf.getType(node.getNodeName());
            }

            if (className!=null) { 
                try {
                    type = Thread.currentThread().getContextClassLoader().loadClass(className);
                } catch (ClassNotFoundException e) {
                    throw new ActivationException("Could not load class " + className + " used by method " 
                            + method.getDeclaringClass() + "." + method.getName());
                }
            } else {
                type = types[types.length-1];
            }

            BeanInfo beanInfo = createBean(obj, node, type);
            args[types.length - 1] = beanInfo.bean;
            if (types.length == 2) {
                args[0] = beanInfo.key;
                log(Level.FINER, "Invoking "
                        + method.getName()
                        + "(" + args[0] + ", " + args[1] + ")");
            } else {
                log(Level.FINER, "Invoking " 
                        + method.getName()
                        + "(" + args[0] + ")");
            }
        } else if (types.length == 0) {
            log(Level.FINER, "Invoking " + method.getName() + "()");
        } else {
            throw new ActivationException("Method " + method.getName() + " has too many arguments");
        }

        try {
            method.invoke(obj, args);
        } catch (IllegalAccessException e) {
            throw new ActivationException("Cannot access method " + method.getName() 
                    + " of " + method.getDeclaringClass());
        } catch (IllegalArgumentException e) {
            /*
               All that can go wrong here is that
               1) an unwrapping conversion for primitive arguments failed, or,
               2) after possible unwrapping, a parameter value cannot be converted to
                  the corresponding formal parameter type by a method invocation conversion. 
            */   
            throw new ActivationException("Cannot invoke " + method.getName() 
                    + " of " + method.getDeclaringClass()
                    + (type == null ? " without an argument" : " with an argument of " + type));
        } catch (InvocationTargetException e) {
            throw new ActivationException("Method " + method.getName()  
                    + " of " + method.getDeclaringClass() + " has thrown an exception", e);
        }

    }

    private String getNodeValue(Node node) {
        StringBuffer s = new StringBuffer();
        if (node.getNodeType()==Node.ELEMENT_NODE) {
            node = node.getFirstChild();
            while (node != null) {
                if (node.getNodeType() == Node.TEXT_NODE || node.getNodeType() == Node.CDATA_SECTION_NODE) {
                    s.append(node.getNodeValue());
                }
                node = node.getNextSibling();
            }
        } else {
            s.append(node.getNodeValue());
        }

        if (trim) {
            return s.toString().trim();
        }
        return s.toString();
        
    }

    /**
     * Fetches the first child element node of a given node.
     */
    private Element getFirstElementChild(Node root) {
        Node node = root.getFirstChild();
        while (node!=null) {
            if (node.getNodeType()==Node.ELEMENT_NODE) {
                return (Element) node;
            }
            node = node.getNextSibling();
        }
        return null;
    }
    
    /**
     * Fetches the next sibling element node of a given node.
     */
    private Element getNextElement(Node root) {
        Node node = root.getNextSibling();
        while (node != null) {
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                return (Element) node;
            }
            node = node.getNextSibling();
        }
        return null;
    }

    /**
     * @return Returns the valueStack.
     */
    public HashMap getValueStack() {
        return valueStack;
    }

    /**
     * The value is used if you use the attribute pop="NUMBER" or "pop#NUMBER" as attribut value.
     * @param newValueStack The valueStack to set.
     */
    public void setValueStack(HashMap newValueStack) {
        valueStack = newValueStack;
    }

}
