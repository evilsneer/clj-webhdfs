(ns webhdfs.core-test
  (:require [clojure.test :refer :all]
            [webhdfs.core :refer :all]
            [clojure.java.io :refer [resource]]))

(def temp-dir "/tmp/")
(def dir-name "webhdfs-test-dir")
(def test-filename "testdata.csv")

(def test-content
  (-> test-filename
    resource
    slurp))

(defn temp-has? [file-name]
  (some (partial = file-name)
    (short-ls temp-dir)))

(deftest test-ls
  (testing "LS returns something"
    (is (some? (not-empty (ls "/"))))))

(deftest test-mkdir
  (testing "MKDIR creates dir"
    (let [path (str temp-dir dir-name "/")]
      (mkdirs path)
      (is (temp-has? dir-name)))))

(deftest test-create-file
  (testing "CREATE creates file"
    (create (str temp-dir test-filename) test-content)
    (is (temp-has? test-filename))))

(deftest test-open-file
  (testing "OPEN created file, check content"
    (is (= test-content (slurp (open (str temp-dir test-filename)))))))

(deftest test-delete
  (testing "DELETE dir"
    (delete (str temp-dir dir-name))
    (is (not (temp-has? dir-name))))
  (testing "DELETE file"
    (delete (str temp-dir test-filename))
    (is (not (temp-has? test-filename)))))

