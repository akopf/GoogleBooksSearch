package com.example.android.googlebookssearch;


public class Book  {

    public final String author;

    public final String title;

    public final String url;

    public Book(String BookAuthor, String BookTitle, String mUrl){
        author = BookAuthor;
        title = BookTitle;
        url = mUrl;
    }

    public String getAuthor() {  return author;
    }

    public String getTitle() {
        return title;
    }


    public String getUrl() {
        return url;
    }
}
