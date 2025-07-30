package com.personal.adoptioncenterapplication

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
import kotlinx.coroutines.tasks.await
import android.graphics.Bitmap
import android.graphics.BitmapFactory

import com.personal.adoptioncenterapplication.model.Animal
import com.personal.adoptioncenterapplication.ui.theme.AdoptionCenterApplicationTheme

// Extensiones de Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File
import java.io.FileOutputStream
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query


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

        // Pantalla lista de animales
        composable("admin_animal_list") {
            AdminAnimalListScreen(
                onNavigateToRequests = { navController.navigate("admin_requests")}
            )
        }

        // Pantalla de solicitudes
        composable("admin_requests") {
            AdminRequestsScreen()
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
                    navController.currentBackStackEntry?.savedStateHandle?.set("animal", selectedAnimal)
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
fun AdminAnimalListScreen(onNavigateToRequests: () -> Unit) {
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
        TextButton(
            onClick = onNavigateToRequests
        ) {
            Text("Solicitudes")
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

// Pantalla para solicitudes
@Composable
fun AdminRequestsScreen() {
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    var requests by remember { mutableStateOf<List<DocumentSnapshot>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Cargar solicitudes al iniciar
    LaunchedEffect(Unit) {
        db.collection("adoption_requests")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(context, "Error al cargar solicitudes", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    requests = snapshot.documents
                    isLoading = false
                }
            }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Solicitudes de Adopción", style = MaterialTheme.typography.headlineMedium)

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(16.dp))
        } else if (requests.isEmpty()) {
            Text("No hay solicitudes.")
        } else {
            LazyColumn {
                items(requests) { doc ->
                    val animalName = doc.getString("animalName") ?: "Desconocido"
                    val fullName = doc.getString("fullName") ?: ""
                    val address = doc.getString("address") ?: ""
                    val notes = doc.getString("notes") ?: ""
                    val status = doc.getString("status") ?: "pendiente"

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Animal: $animalName", style = MaterialTheme.typography.titleMedium)
                            Text("Solicitante: $fullName")
                            Text("Dirección: $address")
                            Text("Comentario: $notes")
                            Text("Estado: $status")

                            if (status == "pendiente") {
                                Button(
                                    onClick = {
                                        doc.reference.update("status", "aprobado")
                                            .addOnSuccessListener {
                                                Toast.makeText(context, "Solicitud aprobada", Toast.LENGTH_SHORT).show()
                                            }
                                            .addOnFailureListener {
                                                Toast.makeText(context, "Error al aprobar", Toast.LENGTH_SHORT).show()
                                            }
                                    },
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Text("Aprobar")
                                }
                            }
                        }
                    }
                }
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
            val bitmap = remember(animal.photoPath) {
                val file = File(animal.photoPath)
                if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
            }

            bitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "Foto de ${animal.name}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentScale = ContentScale.Crop
                )
            }

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
        val bitmap = remember(animal.photoPath) {
            val file = File(animal.photoPath)
            if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
        }

        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = animal.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                contentScale = ContentScale.Crop
            )
        }

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
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()

    var fullName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

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
                if (fullName.isNotBlank() && address.isNotBlank()) {
                    isSubmitting = true

                    val requestData = hashMapOf(
                        "animalName" to animalName,
                        "fullName" to fullName,
                        "address" to address,
                        "notes" to notes,
                        "status" to "pendiente",
                        "timestamp" to Timestamp.now()
                    )

                    db.collection("adoption_requests")
                        .add(requestData)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Solicitud enviada", Toast.LENGTH_SHORT).show()
                            onSubmit()
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Error al enviar solicitud", Toast.LENGTH_SHORT).show()
                        }
                        .addOnCompleteListener {
                            isSubmitting = false
                        }
                } else {
                    Toast.makeText(context, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSubmitting
        ) {
            Text(if (isSubmitting) "Enviando..." else "Enviar Solicitud")
        }
    }
}



// 6. Pantalla de Registro de Animales
@Composable
fun RegisterAnimalScreen(onAnimalRegistered: () -> Unit) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()

    var name by remember { mutableStateOf("") }
    var breed by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var uploading by remember { mutableStateOf(false) }
    var savedImagePath by remember { mutableStateOf<String?>(null) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri -> imageUri = uri }
    )

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

        Button(onClick = { imagePicker.launch("image/*") }) {
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

                    try {
                        val inputStream = context.contentResolver.openInputStream(imageUri!!)
                        val bitmap = BitmapFactory.decodeStream(inputStream)

                        val fileName = "animal_${System.currentTimeMillis()}.jpg"
                        val file = File(context.filesDir, fileName)
                        val outputStream = FileOutputStream(file)

                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)


                        outputStream.flush()
                        outputStream.close()
                        inputStream?.close()

                        savedImagePath = file.absolutePath

                        val animalData = hashMapOf(
                            "name" to name,
                            "breed" to breed,
                            "age" to age,
                            "photoPath" to file.absolutePath
                        )

                        db.collection("animals")
                            .add(animalData)
                            .addOnSuccessListener {
                                uploading = false
                                onAnimalRegistered()
                            }
                            .addOnFailureListener {
                                uploading = false
                                Toast.makeText(context, "Error al guardar en Firebase", Toast.LENGTH_SHORT).show()
                            }

                    } catch (e: Exception) {
                        uploading = false
                        Toast.makeText(context, "Error al guardar imagen local", Toast.LENGTH_SHORT).show()
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