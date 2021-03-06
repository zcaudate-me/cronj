(ns cronj.test-task
    (:use midje.sweet)
    (:require [hara.ova :as ova]
              [clj-time.local :as lt]
              [cronj.data.task :as t] :reload))

(facts "initial values"
  (let [mt (t/task :test-task (fn [& _]))]
    (fact "there are no tasks running"
      (t/running    mt) => ())  ;;
    (fact "there should be no last executed id"
      (t/last-exec  mt) => nil)
    (fact "there should be no last successful id"
      (t/last-successful mt) => nil)
    (fact "output print friendly form"
        (t/<# mt) => {:id :test-task
                      :desc ""
                      :running ()
                      :last-exec nil
                      :last-successful nil})))

(facts "task execution"
  "Setup the data object as well as the tasks that manipulate the data object"
  (let [mda (atom nil)
        mt1 (t/task :mt1 (fn [& _] (reset! mda 1)))
        mt2 (t/task :mt2 (fn [& _] (reset! mda 2)))]

    (let [[mt1-reg mt1-done]  (t/exec! mt1 :mt1-first)]
      @mt1-reg @mt1-done)
    (facts "mt1-first"
      (fact "data should be 1"
        (deref mda) => 1)
      (fact "last-exec should be :mt1-first"
        (t/last-exec mt1) => :mt1-first)
      (fact "last-successful should be :mt1-first"
        (t/last-successful mt1) => :mt1-first)
      (fact "output print friendly form"
        (t/<# mt1) => {:id :mt1
                       :desc ""
                       :running ()
                       :last-exec :mt1-first
                       :last-successful :mt1-first}))

    (let [[mt2-reg mt2-done]  (t/exec! mt2 :mt2-first)]
      @mt2-reg @mt2-done)

    (facts "atfer mt2-first"
      (fact "data should be 2"
        (deref mda) => 2)
      (fact "last-exec should be :mt2-first"
        (t/last-exec mt2) => :mt2-first)
      (fact "last-successful should be :mt2-first"
        (t/last-successful mt2) => :mt2-first))

    (let [[mt1-reg mt1-done]  (t/exec! mt1 :mt1-second)]
      @mt1-reg @mt1-done)
    (facts "after mt1-second"
      (fact "data should be 1"
        (deref mda) => 1)
      (fact "last-exec should be :mt1-second"
        (t/last-exec mt1) => :mt1-second)
      (fact "last-successful should be :mt1-second"
        (t/last-successful mt1) => :mt1-second))))

(facts "longer task execution behaviour"
  (let [job-100ms (t/task :1 (fn [& _] (Thread/sleep 10)))
        [reg job]      (t/exec! job-100ms :test)]
    @reg
    (fact "When the job is still running, it will be in the list of running jobs"
      (t/running job-100ms) => '({:tid :test :opts {}}))
    @job
    (fact "When the job has finished, it will not appear
               in the list of running jobs"
      (t/running job-100ms) => '())))

(facts "multiple threading execution behaviour"
  (let [job (t/task :job (fn [& _] (Thread/sleep 20)))
        [reg1 job1] (t/exec! job :1)
        _           (Thread/sleep 10)
        [reg2 job2] (t/exec! job :2)]
    (do @reg1 @reg2
        (fact "All the jobs are running"
          (t/running job) => '({:tid :1 :opts {}} {:tid :2 :opts {}})
          (t/last-exec job) => :2
          (t/last-successful job) => nil)
        @job1
        (fact "Job 1 will finish"
          (t/running job) => '({:tid :2 :opts {}})
          (t/last-successful job) => :1)
        @job2
        (fact "Job 2 will finish"
          (t/running job) => '()
          (t/last-successful job) => :2))))

(facts "kill!"
  (let [job (t/task :job (fn [& _] (Thread/sleep 2000000)))
        [reg1 job1] (t/exec! job :1)]
    (do @reg1
        (fact "Check that the job is running"
          (t/running job) => '({:tid :1 :opts {}})
          (t/last-exec job) => :1
          (t/last-successful job) => nil)
        (t/kill! job :1)
        (fact "Job 1 will finish"
          (t/running job) => '()
          (t/last-exec job) => :1
          (t/last-successful job) => nil))))

(facts "kill-all!"
  (let [job (t/task :job (fn [_ opts] (Thread/sleep 2000000)))
        [reg1 job1] (t/exec! job :1 {:a1 1 :a2 2})
        [reg2 job2] (t/exec! job :2)]
    (do @reg1
        @reg2
        (fact "Check that the job is running"
          (t/running job) => '({:tid :1 :opts {:a1 1 :a2 2}} {:tid :2 :opts {}})
          (t/last-exec job) => :2
          (t/last-successful job) => nil)
        (t/kill-all! job)
        (fact "Job 1 will finish"
          (t/running job) => '()
          (t/last-exec job) => :2
          (t/last-successful job) => nil)
        (t/reinit! job)
        (fact "Reinitialise tasks"
          (t/running job) => '()
          (t/last-exec job) => nil
          (t/last-successful job) => nil))))
