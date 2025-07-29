package com.personal.adoptioncenterapplication

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.platform.LocalContext
import java.util.UUID
import kotlinx.coroutines.tasks.await
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

import com.personal.adoptioncenterapplication.model.Animal
import com.personal.adoptioncenterapplication.ui.theme.AdoptionCenterApplicationTheme

// Extensiones de Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.firebase.storage.FirebaseStorage


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
    // Controlador de navegación
    val navController = rememberNavController()

    // Configuración de NavHost
    NavHost(
        navController = navController,
        startDestination = "role_selection" // Pantalla inicial
    ) {
        // Pantalla para elegir Admin o User
        composable("role_selection") {
            RoleSelectionScreen(
                onUserSelected = { navController.navigate("login") },
                onAdminSelected = { navController.navigate("admin_login") }
            )
        }

        // 1. Pantalla de Login
        composable("login") {
            LoginScreen(
                onLoginSuccess = { navController.navigate("catalog") },
                onNavigateToRegister = { navController.navigate("register") }
            )
        }

        // Admin login
        composable("admin_login") {
            AdminLoginScreen(
                onAdminLoginSuccess = { navController.navigate("admin_animal_list") },
                onNavigateToRegister = { navController.navigate("register") }
            )
        }

        composable("admin_animal_list") {
            AdminAnimalListScreen()
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
                onAnimalSelected = { selectedAnimal ->
                    navController.navigate("detail")
                },
                onNavigateToRegisterAnimal = {
                    navController.navigate("register_animal")
                }
            )
        }

        // 4. Detalle de Animal
        composable("detail") {
            val animal =
                navController.previousBackStackEntry?.savedStateHandle?.get<Animal>("animal")
            animal?.let {
                AnimalDetailScreen(
                    animal = it,
                    onBack = { navController.popBackStack() },
                    onAdoptClick = { navController.navigate(route = "adoption_request/${animal.name}") }
                )
            }
        }

        // 5. Solicitud de Animal
        composable("adoption_request/{animalName}") { backStackEntry ->
            val animalName = backStackEntry.arguments?.getString("animalName") ?: ""
            AdoptionRequestScreen(
                animalName = animalName,
                onSubmit = { navController.popBackStack() }
            )
        }

        // 6. Registro de Nuevo Animal
        composable("register_animal") {
            RegisterAnimalScreen(
                onAnimalRegistered = { navController.popBackStack() }
            )
        }
    }
}

// PANTALLAS

// Pantalla de inicio
@Composable
fun RoleSelectionScreen(
    onUserSelected: () -> Unit,
    onAdminSelected: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Selecciona tu rol", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onUserSelected,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Entrar como Usuario")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onAdminSelected,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Entrar como Administrador")
        }
    }
}


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

// Admin Login
@Composable
fun AdminLoginScreen(onAdminLoginSuccess: () -> Unit, onNavigateToRegister: () -> Unit) {
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
                            onAdminLoginSuccess()
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

// Pantalla para administrar los registros
@Composable
fun AdminAnimalListScreen() {
    val db = FirebaseFirestore.getInstance()
    var animals by remember { mutableStateOf<List<Pair<String, Animal>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Cargar animales
    LaunchedEffect(Unit) {
        isLoading = true
        val snapshot = db.collection("animals").get().await()
        animals = snapshot.documents.mapNotNull { doc ->
            val animal = doc.toObject(Animal::class.java)
            if (animal != null) doc.id to animal else null
        }
        isLoading = false
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Lista de Animales Registrados", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            CircularProgressIndicator()
        } else {
            LazyColumn {
                items(animals) { (docId, animal) ->
                    AnimalItem(animal = animal, onDelete = {
                        db.collection("animals").document(docId).delete().addOnSuccessListener {
                            animals = animals.filterNot { it.first == docId }
                        }
                    })
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun AnimalItem(animal: Animal, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Nombre: ${animal.name}", style = MaterialTheme.typography.titleMedium)
            Text("Raza: ${animal.breed}")
            Text("Edad: ${animal.age}")
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onDelete, colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.error)) {
                Text("Eliminar")
            }
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
    onAnimalSelected: (Animal) -> Unit,
    onNavigateToRegisterAnimal: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    var animals by remember { mutableStateOf<List<Animal>>(emptyList()) }

    LaunchedEffect(Unit) {
        db.collection("animals")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("Firebase", "Error al obtener animales", error)
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    val list = snapshot.documents.mapNotNull { it.toObject(Animal::class.java) }
                    animals = list
                }
            }
    }

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
    onBack: () -> Unit,
    onAdoptClick: () -> Unit
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

        Row {
            Button(
                onClick = onAdoptClick,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Text("Solicitar Adopción")
            }
            Button(
                onClick = onBack,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Text("Volver")
            }
        }
    }
}

// 5. Pantalla de Solicitud de Animal
@Composable
fun AdoptionRequestScreen(animalName: String, onSubmit: () -> Unit) {
    var fullName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Solicitud de Adopción para $animalName", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = { Text("Nombre completo") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Dirección") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Comentario") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                // Aquí puedes guardar en Firestore si quieres
                onSubmit()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Enviar Solicitud")
        }
    }
}


// 6. Pantalla de Registro de Animales
@Composable
fun RegisterAnimalScreen(onAnimalRegistered: () -> Unit) {
    val context = LocalContext.current
    val storage = FirebaseStorage.getInstance()
    val db = FirebaseFirestore.getInstance()

    var name by remember { mutableStateOf("") }
    var breed by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var uploading by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri -> imageUri = uri }
    )

    fun checkAndPickImage() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_IMAGES
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                context as Activity,
                arrayOf(permission),
                101
            )
        } else {
            launcher.launch("image/*")
        }
    }

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

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = { checkAndPickImage() }) {
            Text("Seleccionar Foto")
        }

        imageUri?.let { uri ->
            Spacer(modifier = Modifier.height(8.dp))
            AsyncImage(
                model = uri,
                contentDescription = "Vista previa",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (name.isNotBlank() && imageUri != null && !uploading) {
                    uploading = true
                    val fileName = UUID.randomUUID().toString()
                    val ref = storage.reference.child("animal_photos/$fileName")

                    ref.putFile(imageUri!!)
                        .continueWithTask { task ->
                            if (!task.isSuccessful) {
                                throw task.exception ?: Exception("Upload failed")
                            }
                            ref.downloadUrl
                        }.addOnSuccessListener { downloadUri ->
                            val animalData = hashMapOf(
                                "nombre" to name,
                                "raza" to breed,
                                "edad" to age,
                                "photoUrl" to downloadUri.toString()
                            )
                            db.collection("animals")
                                .add(animalData)
                                .addOnSuccessListener {
                                    uploading = false
                                    onAnimalRegistered()
                                }
                                .addOnFailureListener {
                                    uploading = false
                                    Toast.makeText(context, "Error al guardar", Toast.LENGTH_SHORT).show()
                                }
                        }.addOnFailureListener {
                            uploading = false
                            Toast.makeText(context, "Error al subir imagen", Toast.LENGTH_SHORT).show()
                        }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uploading
        ) {
            Text(if (uploading) "Guardando..." else "Guardar")
        }
    }
}



@Preview(showBackground = true)
@Composable
fun PreviewScreen() {
    AdoptionCenterApplicationTheme {

    }
}