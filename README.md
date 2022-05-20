This is a gui library intended to somewhat act like the React.js framework for the web.

Instead of useState(), you would use useProperty() which returns what is essentially an atomicreference.
Partitions support useEffect() as well as useEffect() with a cleanup method.

"Issues":
- Could potentially be some unexpected results of inventory methods when dragging items.