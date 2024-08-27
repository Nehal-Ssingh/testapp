import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.*
import com.example.testapp.R
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {
    private lateinit var wordToGuess: String
    private var attemptsLeft = 6

    private val dictionaryApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.dictionaryapi.dev/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DictionaryApi::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val wordDisplay: TextView = findViewById(R.id.wordDisplay)
        val guessInput: EditText = findViewById(R.id.guessInput)
        val submitGuess: Button = findViewById(R.id.submitGuess)
        val resultDisplay: TextView = findViewById(R.id.resultDisplay)

        fetchFiveLetterWords { words ->
            wordToGuess = words.random()

            submitGuess.setOnClickListener {
                val guess = guessInput.text.toString().trim().lowercase()
                if (guess.length != wordToGuess.length) {
                    resultDisplay.text = "Word length must be ${wordToGuess.length}"
                } else {
                    attemptsLeft--
                    checkGuess(guess)
                }
            }
        }
    }

    private fun fetchFiveLetterWords(callback: (List<String>) -> Unit) {
        val potentialWords = listOf("apple", "berry", "chess", "grass", "table")
        val fiveLetterWords = mutableListOf<String>()

        for (word in potentialWords) {
            dictionaryApi.getWordDefinition(word).enqueue(object : Callback<List<WordResponse>> {
                override fun onResponse(call: Call<List<WordResponse>>, response: Response<List<WordResponse>>) {
                    if (response.isSuccessful && response.body() != null) {
                        fiveLetterWords.add(word)
                    }
                    if (fiveLetterWords.size == potentialWords.size) {
                        callback(fiveLetterWords.filter { it.length == 5 })
                    }
                }

                override fun onFailure(call: Call<List<WordResponse>>, t: Throwable) {
                    Toast.makeText(this@MainActivity, "Failed to fetch word definitions", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun checkGuess(guess: String) {
        val resultDisplay: TextView = findViewById(R.id.resultDisplay)
        var result = ""

        for (i in guess.indices) {
            val guessChar = guess[i]
            val correctChar = wordToGuess[i]

            result += when {
                guessChar == correctChar -> "\u2714" // Correct position
                wordToGuess.contains(guessChar) -> "\u23F3" // Correct letter wrong position
                else -> {
                    // ASCII comparison logic
                    val asciiDiff = guessChar.code - correctChar.code
                    if (asciiDiff > 0) {
                        "\u2191" // Guessed character has a higher ASCII value
                    } else {
                        "\u2193" // Guessed character has a lower ASCII value
                    }
                }
            }
        }

        if (guess == wordToGuess) {
            resultDisplay.text = "Congratulations! You guessed the word: $wordToGuess"
        } else if (attemptsLeft == 0) {
            resultDisplay.text = "Game Over! The word was: $wordToGuess"
        } else {
            resultDisplay.text = "$result\nAttempts left: $attemptsLeft"
        }
    }
}
