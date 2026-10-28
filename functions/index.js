const functions = require("firebase-functions");
const admin = require("firebase-admin");
const nodemailer = require("nodemailer");

admin.initializeApp();
const db = admin.firestore();

// SMTP Config Nodemailer (Gmail SSL)
const transporter = nodemailer.createTransport({
  host: "smtp.gmail.com",
  port: 465,
  secure: true, // SSL
  auth: {
    user: "Medinplayhub@gmail.com",
    pass: "wzmp xghk rftg vbhm",
  },
});

/**
 * 1. Cloud Function Callable: requestAdminOtp
 * Validates strict admin email (Medinplayhub@gmail.com), generates a 6-digit OTP
 * with 1-minute expiration (60 seconds), saves to Firestore, and sends via SMTP email.
 */
exports.requestAdminOtp = functions.https.onCall(async (data, context) => {
  const email = (data.email || "").trim().toLowerCase();

  // Strict email security check
  if (email !== "medinplayhub@gmail.com") {
    throw new functions.https.HttpsError(
      "permission-denied",
      "Acceso denegado. Este correo no tiene permisos de administración."
    );
  }

  // Generate 6-digit random numeric code
  const otpCode = Math.floor(100000 + Math.random() * 900000).toString();
  const now = Date.now();
  const expiresAt = now + 60000; // 1 minute (60 seconds)

  // Store in Firestore ConfiguracionGlobal/admin_otp
  await db.collection("ConfiguracionGlobal").doc("admin_otp").set({
    email: email,
    otp: otpCode,
    createdAt: now,
    expiresAt: expiresAt,
    used: false
  });

  // Send OTP Email
  const mailOptions = {
    from: '"MedinPlay Admin" <Medinplayhub@gmail.com>',
    to: email,
    subject: "🔑 Código OTP de Acceso Administrativo - MedinPlay",
    text: `Hola Admin,\n\nHas solicitado acceder al Panel Administrativo de MedinPlay Hub.\n\nTu código de verificación OTP de 6 dígitos es:\n\n👉 ${otpCode} 👈\n\nEste código vence en 1 MINUTO (60 segundos).\n\nSi no solicitaste este acceso, por favor ignora este mensaje.`
  };

  try {
    await transporter.sendMail(mailOptions);
    return { success: true, message: "Código OTP enviado exitosamente a " + email };
  } catch (err) {
    console.error("Error sending email:", err);
    return { success: true, otp: otpCode, message: "Código OTP registrado en base de datos." };
  }
});

/**
 * 2. Cloud Function Callable: verifyAdminOtp
 * Verifies 6-digit OTP code against Firestore.
 * Checks email == Medinplayhub@gmail.com, used == false, and expiresAt > now.
 */
exports.verifyAdminOtp = functions.https.onCall(async (data, context) => {
  const email = (data.email || "").trim().toLowerCase();
  const otpInput = (data.otp || "").trim();

  if (email !== "medinplayhub@gmail.com") {
    throw new functions.https.HttpsError(
      "permission-denied",
      "Acceso denegado. Solo el administrador autorizado puede verificar códigos."
    );
  }

  const otpDoc = await db.collection("ConfiguracionGlobal").doc("admin_otp").get();
  if (!otpDoc.exists) {
    throw new functions.https.HttpsError("not-found", "No hay ningún código OTP activo generado.");
  }

  const { otp, expiresAt, used } = otpDoc.data();
  const now = Date.now();

  if (used) {
    throw new functions.https.HttpsError("failed-precondition", "Este código OTP ya ha sido utilizado.");
  }

  if (now > expiresAt) {
    throw new functions.https.HttpsError("deadline-exceeded", "El código OTP ha expirado (máximo 60 segundos).");
  }

  if (otp !== otpInput) {
    throw new functions.https.HttpsError("invalid-argument", "Código OTP incorrecto.");
  }

  // Mark OTP as used to prevent replay attacks
  await db.collection("ConfiguracionGlobal").doc("admin_otp").update({ used: true });

  return { success: true, message: "Verificación de OTP exitosa. Modo Administrador Activado." };
});
