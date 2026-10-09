package edu.austincollege.sstation.domain;

/** Who owns an account's password (TC-124). */
public enum AuthSource {
  /** A password stored in this app's database. */
  LOCAL,
  /** An AC account; the password lives in AC's directory and is checked there. */
  DIRECTORY
}
