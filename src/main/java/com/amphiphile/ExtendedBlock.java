package com.amphiphile;

class ExtendedBlock {

    public Boolean isIdentified; //идентификатор уже существует в документе
    String id; //идентификатор
    String context; // тип элемента: раздел, абзац, список и т.д.
    int sourceLine; //строка, на которой находится элемент в исходном документе
    String sourceText; // текстовое содержимое элемента
    String marker; // маркер списка (для определения положения элемента в документе в отсутствии номера строки
    String title; // заголовок
    String target; // ссылка на изображение (для img)
    String style;

    ExtendedBlock() {
        isIdentified = false;
    }

}