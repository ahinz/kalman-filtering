# Kalman Filtering

Basic example of a Kalman Filter
from
[CMU](http://biorobotics.ri.cmu.edu/papers/sbp_papers/integrated3/kleeman_kalman_basics.pdf) and
[https://en.wikipedia.org/wiki/Kalman_filter](https://en.wikipedia.org/wiki/Kalman_filter).

This mini clojure example applies a Kalman filter to a falling body
problem. Graphing via d3.

To generate graphs open a repl and run:

```clojure
(in-ns 'kal.core)

(def number-of-data-points 6)
(save number-of-data-points)
```

Then open `assets/viz.html` to see the result. The green line is the
modeled fall, squares are measurements and the red line is the filter
estimate.

![Example Graph](https://github.com/ahinz/kalman-filtering/blob/master/assets/ex.png)

## License

Copyright © 2016 Adam Hinz

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
