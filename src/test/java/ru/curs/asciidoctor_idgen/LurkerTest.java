package ru.curs.asciidoctor_idgen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LurkerTest {
  @Test
  void generateIdFromTitle() {
    assertEquals("_some_section", new Lurker("", true).calcIdFromTitle("Some section"));
  }

}