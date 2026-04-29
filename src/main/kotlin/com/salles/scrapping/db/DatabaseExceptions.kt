package com.salles.scrapping.db

class ProductNameAlreadyExistsException(productName: String) :
    Exception("Product with name '$productName' already exists")

class DatabaseException(cause: Throwable) :
    Exception("Unexpected database error", cause)