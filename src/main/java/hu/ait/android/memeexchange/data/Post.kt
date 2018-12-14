package hu.ait.android.memeexchange.data

data class Post(var uid: String = "",
                var author: String = "",
                var title: String = "",
                var score: Int = 0,
                var imgUrl: String = "",
                var owners: ArrayList<String> = ArrayList())
