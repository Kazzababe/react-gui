package ravioli.gravioli.stategui.gui.partition.state;

public enum RerenderType {
    /**
     * Menus will always re-render when a state change occurs.
     */
    ALWAYS,
    /**
     * Menus will only re-render when a state change occurs IFF the state change would result
     * in a visible change in the menu.
     */
    ONLY_ON_RENDER_CHANGE;
}
