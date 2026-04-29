package com.salles.database

class ProductNameAlreadyExistsException(productName: String) :
    Exception("Product with name '$productName' already exists")

class DatabaseException(cause: Throwable) :
    Exception("Unexpected database error", cause)