package com.example.starstack


import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Find views
        val recyclerView: RecyclerView = findViewById(R.id.recyclerView)
        val searchBar: EditText = findViewById(R.id.searchBar)
        val profileIcon: ImageView = findViewById(R.id.profileIcon)

        // RecyclerView layout
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Movie list
        val movies = listOf(
            Movie("Inception", 4.8, "A thief who enters dreams to steal secrets from deep subconscious.", R.drawable.inception),
            Movie("Interstellar", 4.7, "A team of explorers travel through a wormhole in space to ensure humanity's survival.", R.drawable.interstellar),
            Movie("The Dark Knight", 4.9, "Batman faces the Joker, who wreaks havoc on Gotham City.", R.drawable.dark_knight),
            Movie("Avengers: Endgame", 4.6, "The Avengers assemble for the final battle against Thanos.", R.drawable.endgame),
            Movie("The Matrix", 4.7, "A hacker discovers the shocking truth about reality and fights against the system.", R.drawable.matrix),
            Movie("Titanic", 4.5, "A tragic love story unfolds on the ill-fated Titanic.", R.drawable.titanic),
            Movie("Avatar", 4.6, "A marine on an alien planet struggles between loyalty and morality.", R.drawable.avatar),
            Movie("Spider-Man: No Way Home", 4.5, "Peter Parker faces villains from across the multiverse.", R.drawable.spiderman),
            Movie("Joker", 4.4, "The story of Arthur Fleck and his transformation into the Joker.", R.drawable.joker),
            Movie("Black Panther", 4.3, "T’Challa defends Wakanda as the Black Panther.", R.drawable.black_panther),
            Movie("Gladiator", 4.6, "A former Roman General seeks revenge after being betrayed.", R.drawable.gladiator),
            Movie("Shutter Island", 4.3, "A U.S. Marshal investigates a psychiatric facility on an isolated island.", R.drawable.shutter_island),
            Movie("The Godfather", 5.0, "The aging patriarch of an organized crime dynasty transfers control to his reluctant son.", R.drawable.godfather),
            Movie("The Shawshank Redemption", 4.9, "Two imprisoned men bond over years, finding solace and redemption.", R.drawable.shawshank)
        )

        // Adapter with filtering support
        val adapter = MovieAdapter(movies.toMutableList()) // make it mutable
        recyclerView.adapter = adapter

        // Search functionality
        searchBar.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                adapter.filter(s.toString())
            }
        })

        // Open ProfileActivity
        profileIcon.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }
}
