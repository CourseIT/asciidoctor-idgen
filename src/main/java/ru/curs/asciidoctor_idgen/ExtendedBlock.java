package ru.curs.asciidoctor_idgen;

class ExtendedBlock {
    String docTitle; // заголовок документа
    Boolean isIdentified; //идентификатор уже существует в документе
    String id; //идентификатор
    String parentId;//идентификатор родительского элемента (для списков, таблиц)
    Boolean isEmbeddedDoc; //находится в документе, встроенном в исходный (asciidoc-ячейка)
    String context; // тип элемента: раздел, абзац, список и т.д.
    int sourceLine; //строка, на которой находится элемент в исходном документе
    String sourceText; // текстовое содержимое элемента из исходного документа (asciidoc)
    String htmlText; // содержимое элемента, конвертированное в html
    String term; // Термин
    String description; // Определение символа
    String marker; // маркер списка (для определения положения элемента в документе в отсутствии номера строки
    String title; // заголовок
    String target; // ссылка на изображение (для img)
    String style;

    ExtendedBlock() {
        isIdentified = false;
        isEmbeddedDoc = false;
    }

}