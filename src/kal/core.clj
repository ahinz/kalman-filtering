(ns kal.core
  (:require [clojure.spec :as s]
            [clojure.data.json :as json]))

;; Example from:
;; http://biorobotics.ri.cmu.edu/papers/sbp_papers/integrated3/kleeman_kalman_basics.pdf

;; Cheating for now
(defn minv1x1 [[[a]]]
  [[(/ 1.0 a)]])

(defn minv2x2 [[[a b]
                [c d] :as m]]
  (let [dt (double (- (* a d) (* b c)))
        f #(/ % dt)]
    [[(f d) (f (- b))]
     [(f (- c)) (f a)]]))

(defn minv [v]
  (cond
    (= (count v) 1)
    (minv1x1 v)

    (= (count v) 2)
    (minv2x2 v)

    :else
    (throw (IllegalArgumentException. (str "Oops can't invert " v)))))

(defn sum [xs]
  (reduce + 0.0 xs))

(defn mean [xs]
  (when-not (empty? xs)
    (/ (sum xs) (count xs))))

(defn rotv [xv]
  (apply map vector xv))

(defn meanv [xv]
  (map mean (rotv xv)))

(defn matrix* [m1 m2]
  (into [] (map (fn [m1r]
                  (into [] (map (fn [m2c]
                                  (sum (map * m1r m2c))) (rotv m2)))) m1)))

(defn matrix-map [f m1 m2]
  (into [] (map (fn [m1r m2r]
                  (into [] (map (fn [m1x m2x]
                                  (f m1x m2x))
                                m1r m2r)))
                m1 m2)))

(def matrix- (partial matrix-map -))
(def matrix+ (partial matrix-map +))

(defn scalar* [m s]
  (map (fn [c]
         (map (partial * s) c)) m))

(defn vari [xs]
  (let [u (mean xs)
        n (count xs)]
    (when (> n 1)
      (/ (sum (map #(Math/pow (- % u) 2) xs))
         (- n 1)))))

(defn cov [xs ys]
  (let [n (min (count xs) (count ys))
        n1 (- n 1)
        ux (mean xs)
        uy (mean ys)]
    (when (pos? n1)
      (/ (sum (map (fn [x y] (* (- x ux)
                                (- y uy))) xs ys))
         n1))))

(defn covv [v]
  (let [v' (rotv v)
        n (count v')
        covs (for [c1 v'
                     c2 v']
                 (cov c1 c2))]
    (partition n n covs)))

;; Example:
;;
;; y''(t) = -g
;; y'(t) = y'(t0) - g(t - t0)
;; y(t) = y(t0) + y'(t0)(t - t0) - g/2(t - t0)^2
;;
;; y(k+1) = y(k) + y'(k) - g/2
;;
;; State vector:
;; X(k) = [y(k) y'(k)]
;;
;; F(k) = [1 1
;;         0 1]
;;
;; G(k) = [1/2 1]'
;;
;;
;; X(k-1) = F(k)X(k) + G(k)(-g)
;;
;; Expands into:
;;
;; x0(k+1) = y(k) + y'(k) - 1/2(g)
;; x1(k+1) = y'(k) - g
;;
;;
;; z(k) = measurement stream (I think)

(defn state-prediction [F G U Xk]
  (matrix+ (matrix* F Xk)
           (matrix* G U)))

;; Xsp -> X state prediction
(defn measurement-prediction [H Xsp]
  (matrix* H Xsp))

;; Zk -> actual measurement
;; Zp -> predicted measurement
(defn measurement-residual [Zk Zp]
  (matrix- Zk Zp))

;; V -> measurement-residual
(defn state-estimate [Xsp W V]
  (matrix+ Xsp (matrix* W V)))

(defn state-pred-cov [F Pk Q]
  (matrix+ Q (matrix* (matrix* F Pk) (rotv F))))

(defn measurement-pred-cov [H Pk1 R]
  (matrix+ R (matrix* (matrix* H Pk1) (rotv H))))

(defn filter-gain [Pk1 H Sk1]
  (matrix* (matrix* Pk1 (rotv H))
           (minv Sk1)))

(defn update-state-cov [Pk1 Wk1 Sk1]
  (matrix- Pk1 (matrix* (matrix* Wk1 Pk1)
                        (rotv Wk1))))

(defn step [Zk1 {:keys [Xk Xmodel Pk U F G H Q R]
                 :as state}]
  (let [;; Pk1p -> predicted state cov
        Pk1p (state-pred-cov F Pk Q)
        Sk1 (measurement-pred-cov H Pk1p R)
        Wk1 (filter-gain Pk1p H Sk1)
        ;; Pk1 -> Updated state cov
        Pk1 (update-state-cov Pk1p Wk1 Sk1)

        Xk1p (state-prediction F G U Xk)
        Xmodel1 (state-prediction F G U Xmodel)

        Zk1p (measurement-prediction H Xk1p)
        Vk1 (measurement-residual Zk1 Zk1p)
        Xk1 (state-estimate Xk1p Wk1 Vk1)

        state' (assoc state :Xk Xk1 :Pk Pk1 :Xmodel Xmodel1)]
    state'))


(defn gauss-noise-fn [mean stddev]
  (let [r (java.util.Random.)]
    (fn []
      [[(+ mean (* stddev (.nextGaussian r)))]])))

(defn step-rand [noise-fn {:keys [F G U H Xmodel]
                           :as state}]
  (let [Xmodel1 (state-prediction F G U Xmodel)
        Zk (matrix+ (matrix* H Xmodel1) (noise-fn))]
    (assoc (step Zk state) :Zk Zk)))

(defn iter [initial-state itrs]
  (let [r (gauss-noise-fn 0.0 1.0)
        step (fn [state i]
               (step-rand r (assoc state :iter (inc i))))]
    (reductions step (assoc initial-state :iter 0) (range itrs))))

(def g 1.0)  ;; (really should be 9.80665 but 1.0 makes it a bit to think about)

(def example-state
  ;; F converts from the old state (Xk) to the new state along with G and U
  {:F [[1 1]
       [0 1]]

   ;; Translate control input into state space
   :G [[0.5] [1]]

   :H [[1 0]]

   ;; Control input (in this case, acceleartion)
   :U [[(- g)]
       [(- g)]]

   ;; State noise covariance (stole this from wikipedia)
   :Q [[0.25 0.50]
       [0.50 1.00]]

   ;; Measurement noise covariance
   :R [[1]]

   :Pk [[0.0 0.0]
        [0.0 0.0]]

   :Zk [[100.0]] ;; Initial measurement

   :Xk [[100.0]      ;; x = 100, v = 0
        [0.0]]

   :Xmodel [[100.0]      ;; x = 100, v = 0
            [0.0]]})

(defn save [i]
  (let [s (iter example-state i)
        j (str "var data = " (json/write-str s) ";")]
    (spit "assets/test.js" j)))
