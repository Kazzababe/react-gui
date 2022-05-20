This is a gui library intended to somewhat act like the React.js framework for the web.

Instead of useState(), you would use useProperty() which returns what is essentially an atomicreference.
Partitions support useEffect() as well as useEffect() with a cleanup method.

"Issues":
- Consecutive modifications of state will cause multiple re-renders rather than grouping them into a single re-render.
- Could potentially be some unexpected results of inventory methods when dragging items.