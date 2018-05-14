package com.amphiphile;

public class ExtendedBlock {

    public String id; //идентификатор
    public String context; // тип элемента: раздел, абзац, список и т.д.
    public int sourceLine; //строка, на которой находится элемент в исходном документе
    public String sourceText; // текстовое содержимое элемента
    public String title; // заголовок

}