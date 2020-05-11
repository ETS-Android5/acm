/*
 * @(#)SwingUtils.java	1.02 11/15/08
 *
 */
package org.literacybridge.acm.utils;

import com.github.weisj.darklaf.LafManager;
import com.github.weisj.darklaf.theme.DarculaTheme;
import com.github.weisj.darklaf.theme.IntelliJTheme;
import org.literacybridge.acm.theme.AmplioTheme;

import javax.swing.*;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Point;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A collection of utility methods for Swing.
 *
 * @author Darryl Burke
 *
 * From https://tips4java.wordpress.com/2008/11/13/swing-utils/
 *
 * Commented out the unused parts of the class.
 *
 */
@SuppressWarnings("DanglingJavadoc")
public final class SwingUtils {
    private static final Logger LOG = Logger.getLogger(SwingUtils.class.getName());

    private SwingUtils() {
        throw new Error("SwingUtils is just a container for static methods");
    }

    /**
     * Convenience method for searching below <code>container</code> in the
     * component hierarchy and return nested components that are instances of
     * class <code>clazz</code> it finds. Returns an empty list if no such
     * components exist in the container.
     * <P>
     * Invoking this method with a class parameter of JComponent.class
     * will return all nested components.
     * <P>
     * This method invokes getDescendantsOfType(clazz, container, true)
     *
     * @param clazz the class of components whose instances are to be found.
     * @param container the container at which to begin the search
     * @return the List of components
     */
    public static <T extends JComponent> List<T> getDescendantsOfType(
        Class<T> clazz, Container container) {
        return getDescendantsOfType(clazz, container, true);
    }

    /**
     * Convenience method for searching below <code>container</code> in the
     * component hierarchy and return nested components that are instances of
     * class <code>clazz</code> it finds. Returns an empty list if no such
     * components exist in the container.
     * <P>
     * Invoking this method with a class parameter of JComponent.class
     * will return all nested components.
     *
     * @param clazz the class of components whose instances are to be found.
     * @param container the container at which to begin the search
     * @param nested true to list components nested within another listed
     * component, false otherwise
     * @return the List of components
     */
    private static <T extends JComponent> List<T> getDescendantsOfType(Class<T> clazz,
        Container container,
        boolean nested) {
        List<T> tList = new ArrayList<>();
        for (Component component : container.getComponents()) {
            if (clazz.isAssignableFrom(component.getClass())) {
                tList.add(clazz.cast(component));
            }
            if (nested || !clazz.isAssignableFrom(component.getClass())) {
                tList.addAll(SwingUtils.getDescendantsOfType(clazz,
                    (Container) component, nested));
            }
        }
        return tList;
    }

    /**
     * Convenience method that searches below <code>container</code> in the
     * component hierarchy and returns the first found component that is an
     * instance of class <code>clazz</code> having the bound property value.
     * Returns {@code null} if such component cannot be found.
     * <P>
     * This method invokes getDescendantOfType(clazz, container, property, value,
     * true)
     *
     * @param clazz the class of component whose instance is to be found.
     * @param container the container at which to begin the search
     * @param property the className of the bound property, exactly as expressed in
     * the accessor e.g. "Text" for getText(), "Value" for getValue().
     * @param value the value of the bound property
     * @return the component, or null if no such component exists in the
     * container
     * @throws java.lang.IllegalArgumentException if the bound property does
     * not exist for the class or cannot be accessed
     */
//    public static <T extends JComponent> T getDescendantOfType(
//        Class<T> clazz, Container container, String property, Object value)
//        throws IllegalArgumentException {
//        return getDescendantOfType(clazz, container, property, value, true);
//    }

    /**
     * Convenience method that searches below <code>container</code> in the
     * component hierarchy and returns the first found component that is an
     * instance of class <code>clazz</code> and has the bound property value.
     * Returns {@code null} if such component cannot be found.
     *
     * @param clazz the class of component whose instance to be found.
     * @param container the container at which to begin the search
     * @param property the className of the bound property, exactly as expressed in
     * the accessor e.g. "Text" for getText(), "Value" for getValue().
     * @param value the value of the bound property
     * @param nested true to list components nested within another component
     * which is also an instance of <code>clazz</code>, false otherwise
     * @return the component, or null if no such component exists in the
     * container
     * @throws java.lang.IllegalArgumentException if the bound property does
     * not exist for the class or cannot be accessed
     */
//    public static <T extends JComponent> T getDescendantOfType(Class<T> clazz,
//        Container container, String property, Object value, boolean nested)
//        throws IllegalArgumentException {
//        List<T> list = getDescendantsOfType(clazz, container, nested);
//        return getComponentFromList(clazz, list, property, value);
//    }

    /**
     * Convenience method for searching below <code>container</code> in the
     * component hierarchy and return nested components of class
     * <code>clazz</code> it finds.  Returns an empty list if no such
     * components exist in the container.
     * <P>
     * This method invokes getDescendantsOfClass(clazz, container, true)
     *
     * @param clazz the class of components to be found.
     * @param container the container at which to begin the search
     * @return the List of components
     */
//    public static <T extends JComponent> List<T> getDescendantsOfClass(
//        Class<T> clazz, Container container) {
//        return getDescendantsOfClass(clazz, container, true);
//    }

    /**
     * Convenience method for searching below <code>container</code> in the
     * component hierarchy and return nested components of class
     * <code>clazz</code> it finds.  Returns an empty list if no such
     * components exist in the container.
     *
     * @param clazz the class of components to be found.
     * @param container the container at which to begin the search
     * @param nested true to list components nested within another listed
     * component, false otherwise
     * @return the List of components
     */
//    public static <T extends JComponent> List<T> getDescendantsOfClass(
//        Class<T> clazz, Container container, boolean nested) {
//        List<T> tList = new ArrayList<T>();
//        for (Component component : container.getComponents()) {
//            if (clazz.equals(component.getClass())) {
//                tList.add(clazz.cast(component));
//            }
//            if (nested || !clazz.equals(component.getClass())) {
//                tList.addAll(SwingUtils.<T>getDescendantsOfClass(clazz,
//                    (Container) component, nested));
//            }
//        }
//        return tList;
//    }

    /**
     * Convenience method that searches below <code>container</code> in the
     * component hierarchy in a depth first manner and returns the first
     * found component of class <code>clazz</code> having the bound property
     * value.
     * <P>
     * Returns {@code null} if such component cannot be found.
     * <P>
     * This method invokes getDescendantOfClass(clazz, container, property,
     * value, true)
     *
     * @param clazz the class of component to be found.
     * @param container the container at which to begin the search
     * @param property the className of the bound property, exactly as expressed in
     * the accessor e.g. "Text" for getText(), "Value" for getValue().
     * This parameter is case sensitive.
     * @param value the value of the bound property
     * @return the component, or null if no such component exists in the
     * container's hierarchy.
     * @throws java.lang.IllegalArgumentException if the bound property does
     * not exist for the class or cannot be accessed
     */
//    public static <T extends JComponent> T getDescendantOfClass(Class<T> clazz,
//        Container container, String property, Object value)
//        throws IllegalArgumentException {
//        return getDescendantOfClass(clazz, container, property, value, true);
//    }

    /**
     * Convenience method that searches below <code>container</code> in the
     * component hierarchy in a depth first manner and returns the first
     * found component of class <code>clazz</code> having the bound property
     * value.
     * <P>
     * Returns {@code null} if such component cannot be found.
     *
     * @param clazz the class of component to be found.
     * @param container the container at which to begin the search
     * @param property the className of the bound property, exactly as expressed
     * in the accessor e.g. "Text" for getText(), "Value" for getValue().
     * This parameter is case sensitive.
     * @param value the value of the bound property
     * @param nested true to include components nested within another listed
     * component, false otherwise
     * @return the component, or null if no such component exists in the
     * container's hierarchy
     * @throws java.lang.IllegalArgumentException if the bound property does
     * not exist for the class or cannot be accessed
     */
//    public static <T extends JComponent> T getDescendantOfClass(Class<T> clazz,
//        Container container, String property, Object value, boolean nested)
//        throws IllegalArgumentException {
//        List<T> list = getDescendantsOfClass(clazz, container, nested);
//        return getComponentFromList(clazz, list, property, value);
//    }

//    private static <T extends JComponent> T getComponentFromList(Class<T> clazz,
//        List<T> list, String property, Object value)
//        throws IllegalArgumentException {
//        T retVal = null;
//        Method method = null;
//        try {
//            method = clazz.getMethod("get" + property);
//        } catch (NoSuchMethodException ex) {
//            try {
//                method = clazz.getMethod("is" + property);
//            } catch (NoSuchMethodException ex1) {
//                throw new IllegalArgumentException("Property " + property +
//                    " not found in class " + clazz.getName());
//            }
//        }
//        try {
//            for (T t : list) {
//                Object testVal = method.invoke(t);
//                if (equals(value, testVal)) {
//                    return t;
//                }
//            }
//        } catch (InvocationTargetException ex) {
//            throw new IllegalArgumentException(
//                "Error accessing property " + property +
//                    " in class " + clazz.getName());
//        } catch (IllegalAccessException ex) {
//            throw new IllegalArgumentException(
//                "Property " + property +
//                    " cannot be accessed in class " + clazz.getName());
//        } catch (SecurityException ex) {
//            throw new IllegalArgumentException(
//                "Property " + property +
//                    " cannot be accessed in class " + clazz.getName());
//        }
//        return retVal;
//    }

    /**
     * Convenience method for determining whether two objects are either
     * equal or both null.
     *
     * @param obj1 the first reference object to compare.
     * @param obj2 the second reference object to compare.
     * @return true if obj1 and obj2 are equal or if both are null,
     * false otherwise
     */
    public static boolean equals(Object obj1, Object obj2) {
        return Objects.equals(obj1, obj2);
    }

    /**
     * Convenience method for mapping a container in the hierarchy to its
     * contained components.  The keys are the containers, and the values
     * are lists of contained components.
     * <P>
     * Implementation note:  The returned value is a HashMap and the values
     * are of type ArrayList.  This is subject to change, so callers should
     * code against the interfaces Map and List.
     *
     * @param container The JComponent to be mapped
     * @param nested true to drill down to nested containers, false otherwise
     * @return the Map of the UI
     */
//    public static Map<JComponent, List<JComponent>> getComponentMap(
//        JComponent container, boolean nested) {
//        HashMap<JComponent, List<JComponent>> retVal =
//            new HashMap<JComponent, List<JComponent>>();
//        for (JComponent component : getDescendantsOfType(JComponent.class,
//            container, false)) {
//            if (!retVal.containsKey(container)) {
//                retVal.put(container,
//                    new ArrayList<JComponent>());
//            }
//            retVal.get(container).add(component);
//            if (nested) {
//                retVal.putAll(getComponentMap(component, nested));
//            }
//        }
//        return retVal;
//    }

    /**
     * Convenience method for retrieving a subset of the UIDefaults pertaining
     * to a particular class.
     *
     * @param clazz the class of interest
     * @return the UIDefaults of the class
     */
//    public static UIDefaults getUIDefaultsOfClass(Class clazz) {
//        String name = clazz.getName();
//        name = name.substring(name.lastIndexOf(".") + 2);
//        return getUIDefaultsOfClass(name);
//    }

    /**
     * Convenience method for retrieving a subset of the UIDefaults pertaining
     * to a particular class.
     *
     * @param className fully qualified name of the class of interest
     * @return the UIDefaults of the class named
     */
//    public static UIDefaults getUIDefaultsOfClass(String className) {
//        UIDefaults retVal = new UIDefaults();
//        UIDefaults defaults = UIManager.getLookAndFeelDefaults();
//        List<?> listKeys = Collections.list(defaults.keys());
//        for (Object key : listKeys) {
//            if (key instanceof String && ((String) key).startsWith(className)) {
//                String stringKey = (String) key;
//                String property = stringKey;
//                if (stringKey.contains(".")) {
//                    property = stringKey.substring(stringKey.indexOf(".") + 1);
//                }
//                retVal.put(property, defaults.get(key));
//            }
//        }
//        return retVal;
//    }

    /**
     * Convenience method for retrieving the UIDefault for a single property
     * of a particular class.
     *
     * @param clazz the class of interest
     * @param property the property to query
     * @return the UIDefault property, or null if not found
     */
//    public static Object getUIDefaultOfClass(Class clazz, String property) {
//        Object retVal = null;
//        UIDefaults defaults = getUIDefaultsOfClass(clazz);
//        List<Object> listKeys = Collections.list(defaults.keys());
//        for (Object key : listKeys) {
//            if (key.equals(property)) {
//                return defaults.get(key);
//            }
//            if (key.toString().equalsIgnoreCase(property)) {
//                retVal = defaults.get(key);
//            }
//        }
//        return retVal;
//    }

    /**
     * Exclude methods that return values that are meaningless to the user
     */
//    private static Set<String> setExclude = new HashSet<String>();
//    static {
//        setExclude.add("getFocusCycleRootAncestor");
//        setExclude.add("getAccessibleContext");
//        setExclude.add("getColorModel");
//        setExclude.add("getGraphics");
//        setExclude.add("getGraphicsConfiguration");
//    }

    /**
     * Common code to set L&F.
     * @param laf Short name of desired L&F.
     */
    public static void setLookAndFeel(String laf) {
        /*
         * Add this to your application level JPanel derivative:
                @Override
                public void setBackground(Color bgColor) {
                    // Workaround for weird bug in seaglass look&feel that causes a
                    // java.awt.IllegalComponentStateException when e.g. a combo box
                    // in this dialog is clicked on
                    if (bgColor.getAlpha() == 0) {
                        super.setBackground(backgroundColor);
                    } else {
                        super.setBackground(bgColor);
                        backgroundColor = bgColor;
                    }
                }
         */

        // Not sure why, but making this call before setting the seaglass look and
        // feel
        // prevents an UnsatisfiedLinkError to be thrown
        final LookAndFeel defaultLAF = UIManager.getLookAndFeel();
        if (laf.equalsIgnoreCase("nimbus")) {
            try {
                for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                    if ("Nimbus".equals(info.getName())) {

                        // TODO: Find enough overrides to go here to make nimbus look acceptable.
                        // Background colors are the big thing. Also need a scroll bar painter.
                        UIManager.put("control", Color.white);

                        UIManager.setLookAndFeel(info.getClassName());
                        System.out.println("Set l&f to Nimbus.");
                        return;
                    }
                }
            } catch (Exception e) {
                // If Nimbus is not available, you can set the GUI to another look and feel.
            }
        }
        if (laf.equalsIgnoreCase("metal")) {
            //"javax.swing.plaf.metal.MetalLookAndFeel"
            try {
                // select Look and Feel
                UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
                // start application
                System.out.println("Set l&f to metal.");
                return;
            }
            catch (Exception ignored) {
            }
        }
        if (laf.equalsIgnoreCase("darklaf")) {
            //"com.github.weisj:darklaf-core"
            try {
                // select Look and Feel
                // This returns the preferred theme of the system settings.
//                LafManager.install(new AmplioTheme());
                LafManager.install(new IntelliJTheme());
//                LafManager.setTheme(new DarculaTheme());
                // start application
                System.out.printf("Set l&f to %s.\n", LafManager.getTheme().getName());
                return;
            }
            catch (Exception ignored) {
            }
        }

        // Fall back to SeaGlass
        try {
            UIManager.setLookAndFeel("com.seaglasslookandfeel.SeaGlassLookAndFeel");
        } catch (Exception e) {
            try {
                LOG.log(Level.WARNING, "Unable to set look and feel.", e);
                UIManager.setLookAndFeel(defaultLAF);
            } catch (Exception e1) {
                LOG.log(Level.WARNING, "Unable to set look and feel.", e1);
            }
        }
    }

    /**
     * Convenience method for obtaining most non-null human readable properties
     * of a JComponent.  Array properties are not included.
     * <P>
     * Implementation note:  The returned value is a HashMap.  This is subject
     * to change, so callers should code against the interface Map.
     *
     * @param component the component whose proerties are to be determined
     * @return the class and value of the properties
     */
//    public static Map<Object, Object> getProperties(JComponent component) {
//        Map<Object, Object> retVal = new HashMap<Object, Object>();
//        Class<?> clazz = component.getClass();
//        Method[] methods = clazz.getMethods();
//        Object value = null;
//        for (Method method : methods) {
//            if (method.getName().matches("^(is|get).*") &&
//                method.getParameterTypes().length == 0) {
//                try {
//                    Class returnType = method.getReturnType();
//                    if (returnType != void.class &&
//                        !returnType.getName().startsWith("[") &&
//                        !setExclude.contains(method.getName())) {
//                        String key = method.getName();
//                        value = method.invoke(component);
//                        if (value != null && !(value instanceof Component)) {
//                            retVal.put(key, value);
//                        }
//                    }
//                    // ignore exceptions that arise if the property could not be accessed
//                } catch (IllegalAccessException ex) {
//                } catch (IllegalArgumentException ex) {
//                } catch (InvocationTargetException ex) {
//                }
//            }
//        }
//        return retVal;
//    }

    /**
     * Convenience method to obtain the Swing class from which this
     * component was directly or indirectly derived.
     *
     * @param component The component whose Swing superclass is to be
     * determined
     * @return The nearest Swing class in the inheritance tree
     */
//    public static <T extends JComponent> Class getJClass(T component) {
//        Class<?> clazz = component.getClass();
//        while (!clazz.getName().matches("javax.swing.J[^.]*$")) {
//            clazz = clazz.getSuperclass();
//        }
//        return clazz;
//    }

    /**
     * Given a component, return it's location relative to its root owner.
     * @param component for which to get the location.
     * @return the location.
     */
    public static Point getApplicationRelativeLocation(Component component) {
        Point pComponent = component.getLocation();
        if (component.getParent() != null) {
            Point pParent = getApplicationRelativeLocation(component.getParent());
            pComponent.x += pParent.x;
            pComponent.y += pParent.y;
        }
        return pComponent;
    }

    /**
     * Handles otherwise unhandled Esc in dialogs.
     * @param dialog to which to add the listener.
     */
    public static void addEscapeListener(final JDialog dialog) {
        ActionListener escListener = e -> dialog.setVisible(false);

        dialog.getRootPane().registerKeyboardAction(escListener,
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW);

    }
}
