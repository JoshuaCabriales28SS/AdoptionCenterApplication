package com.personal.adoptioncenterapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage

import com.personal.adoptioncenterapplication.model.Animal
import com.personal.adoptioncenterapplication.ui.theme.AdoptionCenterApplicationTheme

// Extensiones de Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.FirebaseApp
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Inicializar explicitamente conexion con Firebase
        FirebaseApp.initializeApp(this)

        setContent {
            AdoptionCenterApplicationTheme {
                AdoptionApp()
            }
        }
    }
}

@Composable
fun AdoptionApp() {
    // 1. Controlador de navegación
    val navController = rememberNavController()

    // 2. Datos de ejemplo
    val Animals = remember {
        listOf(
            Animal(
                id = 1,
                name = "Max",
                breed = "Labrador",
                age = "2 años",
                photoUrl = "",
                description = "",
                vaccinated = false
            ),
            Animal(
                id = 2,
                name = "Luna",
                breed = "Siamesa",
                age = "1 año",
                photoUrl = "",
                description = "",
                vaccinated = false
            )
        )
    }

    // 3. Configuración de NavHost
    NavHost(
        navController = navController,
        startDestination = "login" // Pantalla inicial
    ) {
        // 1. Pantalla de Login
        composable("login") {
            LoginScreen(
                onLoginSuccess = { navController.navigate("catalog") },
                onNavigateToRegister = { navController.navigate("register") }
            )
        }

        // 2. Pantalla de Registro de Usuario
        composable("register") {
            RegisterScreen(
                onRegisterComplete = { navController.navigate("catalog") }
            )
        }

        // 3. Catálogo de Animales
        composable("catalog") {
            AnimalCatalogScreen(
                animals = Animals,
                onAnimalSelected = { animal ->
                    // Envía el animal seleccionado a la pantalla de detalle
                    navController.currentBackStackEntry?.savedStateHandle?.set("animal", animal)
                    navController.navigate("detail")
                },
                onNavigateToRegisterAnimal = { navController.navigate("register_animal") }
            )
        }

        // 4. Detalle de Animal
        composable("detail") {
            val animal =
                navController.previousBackStackEntry?.savedStateHandle?.get<Animal>("animal")
            animal?.let {
                AnimalDetailScreen(
                    animal = it,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        // 5. Registro de Nuevo Animal
        composable("register_animal") {
            RegisterAnimalScreen(
                onAnimalRegistered = { navController.popBackStack() }
            )
        }
    }
}

// PANTALLAS

// 1. Pantalla de Login
@Composable
fun LoginScreen(onLoginSuccess: () -> Unit, onNavigateToRegister: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Inicio de Sesión", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { if (email.isNotBlank() && password.isNotBlank()) {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(context, "Bienvenido", Toast.LENGTH_SHORT).show()
                            onLoginSuccess()
                        } else {
                            Toast.makeText(
                                context,
                                "Error: ${task.exception?.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                } else {
                    Toast.makeText(context, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primary)
                .fillMaxWidth()
        ) {
            Text("Ingresar")
        }
        TextButton(
            onClick = onNavigateToRegister
        ) {
            Text("¿No tienes cuenta? Regístrate")
        }
    }
}

// 2. Pantalla de Registro de Usuario
@Composable
fun RegisterScreen(onRegisterComplete: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Crear Cuenta", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nombre Completo") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(text = "Contraseña") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                if (name.isNotBlank() && email.isNotBlank() && password.isNotBlank()) {
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(
                                    context,
                                    "Usuario creado correctamente",
                                    Toast.LENGTH_SHORT
                                ).show()
                                onRegisterComplete()
                            } else {
                                Toast.makeText(context, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                } else {
                    Toast.makeText(context, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Registrarse")
        }
    }
}

// 3. Pantalla de Catalogo de Animales
@Composable
fun AnimalCatalogScreen(
    animals: List<Animal>,
    onAnimalSelected: (Animal) -> Unit,
    onNavigateToRegisterAnimal: () -> Unit
) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToRegisterAnimal,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Agregar animal")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(8.dp)
        ) {
            items(animals) { animal ->
                AnimalCard(
                    animal = animal,
                    onClick = { onAnimalSelected(animal) }
                )
            }
        }
    }
}

@Composable
fun AnimalCard(
    animal: Animal,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column {
            AsyncImage(
                model = animal.photoUrl,
                contentDescription = "Foto de ${animal.name}",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = animal.name,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Text(
                text = "Raza: ${animal.breed}",
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Text(
                text = "Edad: ${animal.age}",
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

// 4. Pantalla de Detalle de Animal
@Composable
fun AnimalDetailScreen(
    animal: Animal,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        AsyncImage(
            model = animal.photoUrl,
            contentDescription = animal.name,
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(animal.name, style = MaterialTheme.typography.headlineMedium)
        Text("Raza: ${animal.breed}")
        Text("Edad: ${animal.age}")
        Spacer(modifier = Modifier.height(16.dp))
        Text("Descripción: Lorem ipsum dolor sit amet...")
        Button(onClick = onBack, modifier = Modifier.align(Alignment.End)) {
            Text("Volver")
        }
    }
}

// 5. Pantalla de Registro de Animales
@Composable
fun RegisterAnimalScreen(onAnimalRegistered: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var breed by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Registrar Animal", style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nombre") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = breed,
            onValueChange = { breed = it },
            label = { Text("Raza") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = age,
            onValueChange = { age = it },
            label = { Text("Edad") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { if (name.isNotBlank()) onAnimalRegistered() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Guardar")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewScreen() {
    AdoptionCenterApplicationTheme {

    }
}