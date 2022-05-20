# Reactive GUI Library
#### _A GUI library that attempts to emulate some of the features of React.js_

## Features

- Smart re-rendering of menu partitions based off state.
- Functionally design menus easily.

## Installation
xd

## Basic Usage
Below are several examples to get you started on developing menus.

## Creating a menu
Creating menus is easy and fast. The below snippet shows off creating a menu with a height of 6 rows, a basic title, and a unique identifier with the key "id".

```java
Menu.openMenu(new NamespacedKey(plugin, "id"), plugin, player, (menuPartition) -> {
    menuPartition.setHeight(6);
    menuPartition.setTitle(Component.text("Basic title"));
});
```

## Adding items to the menu
Items are set by simply calling setSlot (and other methods) on any given partition. You can define where the item goes, what the item is, and what happens when you click on it.
```java
Menu.openMenu(new NamespacedKey(plugin, "id"), plugin, player, (menuPartition) -> {
    // .... //
    menuPartition.setSlot(0, new ItemStack(Material.GOLD_BLOCK), (event) -> {
        player.sendMessage("You clicked me!");
    });
});
```

## Creating partitions
Partitions are simply areas inside of the menu that can be defined and handled separately from the primary partition (root menu).
The example below creates a simple 3x3 partition positioned at (1, 1) or slot 10 of the menu. All items created inside of a partition are positioned relative to the partition itself.
The second partition (useMaskedPartition) creates a new partition that allows you to define a mask for allowed items. In this example we create a mask that represens a + symbol and fill it with stained glass panes.

```java
Menu.openMenu(new NamespacedKey(plugin, "id"), plugin, player, (menuPartition) -> {
    // .... //
    menuPartition.usePartition((partition) -> {
        partition.setDimensions(3, 3);
        partition.setPosition(1, 1);
        
        menuPartition.setSlot(0, new ItemStack(Material.GOLD_BLOCK), (event) -> {
            player.sendMessage("You clicked me!");
        });
    });
    menuPartition.useMaskedPartition((partition) -> {
        partition.setDimensions(3, 3);
        partition.setPosition(1, 1);
        partition.setMask("010 111 010");
        
        for (int i = 0; i < partition.getMaskSize(); i++) {
            partition.fillItem(new ItemStack(Material.GRAY_STAINED_GLASS_PANE));    
        }
    });
});
```

## State
State works similary to how it would work in React.js. You create it with a partition, and whenever the state is modified, any partitions affected by that state change will re-render. The aforementioned is the default behavior but you can also explicitly tell the menu to re-render regardless of whether the state change visibly changes the menu.

```java
Menu.openMenu(new NamespacedKey(plugin, "id"), plugin, player, (menuPartition) -> {
    final MenuProperty<Integer> count = menuPartition.useProperty(0);
    
    // By default, without the below line (or similar) changing count will not actually trigger a re-render as the menu does not actually change
    menuPartition.setTitle(Component.text("Clicked " + count.get() + " times.")); 
    menuPartition.setSlot(0, new ItemStack(Material.GOLD_BLOCK), (event) -> {
        count.set(count.get() + 1);
    });
    
    // You can also batch state changes using the below code in order to avoid multiple re-renders
    // This is reccomended behavior but is not required
    menuPartition.batchPropertyChanges(() -> {
        count.set(0);
        count.set(1);
        
        // Normally this would trigger two consecutive re-renders, but by batching them, we only cause one.
    });
    
    // menuPartition.setRerenderType(RerenderType.ALWAYS); // The default is RerenderType.ONLY_ON_RENDER_CHANGE
});
```

## Effects
Sometimes you want to run some code after a partition is done rendering. Similar to React.js you can use ``useEffect`` to do so.
In the below snippet, we create an effect that updates ``onlinePlayers`` to be the count of all online players. Once ``onlinePlayers`` is changed and triggers a re-render, the cleanup task -- ``task::cancel`` -- will be called and upon the next re-render the effect will be called again.
If you notice that as a second argument to ``useEffect``, we include ``onlinePlayers``. This simply means that the effect will not be called unless the property ``onlinePlayers`` is modified. That way we can maintain the scheduler for as long as possible and only start/cancel it when we need to.

```java
Menu.openMenu(new NamespacedKey(plugin, "id"), plugin, player, (menuPartition) -> {
    final MenuProperty<Integer> onlinePlayers = menuPartition.useProperty(0);
    
    menuPartition.useEffect(() -> {
        final BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            onlinePlayers.set(Bukkit.getOnlinePlayers().size());
        }, 20, 20);
        
        return task::cancel;
    }, onlinePlayers);
    menuPartition.setTitle(Component.text("Online players: " + onlinePlayers.get())); 
});
```