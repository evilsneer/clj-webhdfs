(ns webhdfs.core-test
  (:require [clojure.test :refer :all]
            [webhdfs.core :refer :all]
            [clojure.java.io :refer [resource]]))

(def temp-dir "/tmp/")
(def dir-name "webhdfs-test-dir")
(def test-filename "testdata.csv")
(def dir-path (str temp-dir dir-name "/"))
(def file-path (str temp-dir test-filename))

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
    (mkdirs dir-path)
    (is (temp-has? dir-name))))

(deftest test-create-file
  (testing "CREATE creates file"
    (create file-path test-content)
    (is (temp-has? test-filename))
    (testing "file user as in ENV"
      (if (some? username)
        (is (=
              username
              (->> file-path
                ls
                first
                :owner)))))))

(deftest test-open-file
  (testing "OPEN created file, check content"
    (is (= test-content (slurp (open file-path))))))

(deftest test-delete
  (testing "DELETE dir"
    (delete dir-path)
    (is (not (temp-has? dir-name))))
  (testing "DELETE file"
    (delete file-path)
    (is (not (temp-has? test-filename)))))

