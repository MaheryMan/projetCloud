<template>
    <ion-page>
        <ion-content class="login-content" :fullscreen="true">
            <div class="login-container">
                <!-- Logo/Brand Section -->
                <div class="brand-section">
                    <div class="logo-circle">
                        <ion-icon :icon="lockClosedOutline" class="logo-icon"></ion-icon>
                    </div>
                    <h1 class="brand-title">Bienvenue</h1>
                    <p class="brand-subtitle">Connectez-vous à votre compte</p>
                </div>

                <!-- Login Form -->
                <div class="form-section">
                    <ion-card class="login-card">
                        <ion-card-content>
                            <ion-item lines="none" class="input-item">
                                <ion-icon :icon="mailOutline" slot="start" class="input-icon"></ion-icon>
                                <ion-input v-model="email" label="Email" label-placement="floating" type="email"
                                    placeholder="email@exemple.com" />
                            </ion-item>

                            <ion-item lines="none" class="input-item">
                                <ion-icon :icon="lockClosedOutline" slot="start" class="input-icon"></ion-icon>
                                <ion-input v-model="password" label="Mot de passe" label-placement="floating"
                                    :type="showPassword ? 'text' : 'password'" />
                                <ion-button fill="clear" slot="end" @click="showPassword = !showPassword"
                                    class="password-toggle">
                                    <ion-icon :icon="showPassword ? eyeOffOutline : eyeOutline"
                                        slot="icon-only"></ion-icon>
                                </ion-button>
                            </ion-item>

                            <ion-text color="danger" class="error-text" v-if="error">
                                <ion-icon :icon="alertCircleOutline"></ion-icon>
                                {{ error }}
                            </ion-text>

                            <ion-button expand="block" @click="handleLogin" :disabled="loading" class="login-button">
                                <ion-spinner name="crescent" v-if="loading"></ion-spinner>
                                <span v-else>Se connecter</span>
                            </ion-button>

                            <div class="forgot-password">
                                <ion-button fill="clear" size="small">
                                    Mot de passe oublié ?
                                </ion-button>
                            </div>

                            <!-- Divider -->
                            <div class="divider">
                                <span class="divider-text">OU</span>
                            </div>

                            <!-- Google Login -->
                            <ion-button expand="block" @click="loginGoogle" :disabled="loadingGoogle"
                                class="google-button">
                                <ion-icon :icon="logoGoogle" slot="start"></ion-icon>
                                <ion-spinner name="crescent" v-if="loadingGoogle"></ion-spinner>
                                <span v-else>Se connecter avec Google</span>
                            </ion-button>

                            <!-- Bouton pour accéder à l'accueil sans connexion -->
                            <ion-button expand="block" fill="outline" @click="goToHomeWithoutLogin" class="guest-button">
                                Continuer sans se connecter
                            </ion-button>
                        </ion-card-content>
                    </ion-card>
                </div>
            </div>
        </ion-content>
    </ion-page>
</template>

<script setup lang="ts">
import {
    IonPage,
    IonContent,
    IonItem,
    IonInput,   
    IonButton,
    IonText,
    IonIcon,
    IonCard,
    IonCardContent,
    IonSpinner
} from '@ionic/vue'

import {
    mailOutline,
    lockClosedOutline,
    eyeOutline,
    eyeOffOutline,
    alertCircleOutline,
    logoGoogle
} from 'ionicons/icons'

import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useAuthStore } from '@/stores/auth.store'
import { useToast } from '@/composables/useToast'
import { useErrorHandler } from '@/composables/useErrorHandler'
import { useFormValidation } from '@/composables/useFormValidation'

const email = ref('')
const password = ref('')
const showPassword = ref(false)
const router = useRouter()

const authStore = useAuthStore()
const { loading, error } = storeToRefs(authStore)
const { showToast } = useToast()
const { handleError, clearError } = useErrorHandler()

const { errors, hasErrors } = useFormValidation({ email, password })

const handleLogin = async () => {
    clearError()

    if (hasErrors.value) {
        showToast('Veuillez remplir tous les champs obligatoires', 'warning')
        return
    }

    try {
        await authStore.loginWithEmail(email.value, password.value)
        showToast('Connexion réussie', 'success')
        router.replace('/home')
    } catch (err) {
        handleError(err, 'Email ou mot de passe incorrect')
    }
}

const loginGoogle = async () => {
    clearError()

    try {
        await authStore.loginGoogleAction()
        showToast('Connexion avec Google réussie', 'success')
        router.replace('/home')
    } catch (err) {
        handleError(err, 'Erreur lors de la connexion avec Google')
    }
}

const goToHomeWithoutLogin = () => {
    router.push('/home')
}

</script>

<style scoped>
.login-content {
    --background: linear-gradient(135deg, #0a1929 0%, #1a2332 50%, #0f2847 100%);
}

.login-container {
    display: flex;
    flex-direction: column;
    justify-content: center;
    min-height: 100%;
    padding: 20px;
}

/* Brand Section */
.brand-section {
    text-align: center;
    margin-bottom: 40px;
    animation: fadeInDown 0.6s ease-out;
}

.logo-circle {
    width: 80px;
    height: 80px;
    margin: 0 auto 20px;
    background: linear-gradient(135deg, #1e3a8a 0%, #3b82f6 100%);
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 8px 24px rgba(59, 130, 246, 0.3);
}

.logo-icon {
    font-size: 40px;
    color: white;
}

.brand-title {
    font-size: 32px;
    font-weight: 700;
    color: white;
    margin: 0 0 8px 0;
}

.brand-subtitle {
    font-size: 16px;
    color: rgba(255, 255, 255, 0.7);
    margin: 0;
}

/* Form Section */
.form-section {
    animation: fadeInUp 0.6s ease-out;
}

.login-card {
    background: rgba(255, 255, 255, 0.95);
    backdrop-filter: blur(10px);
    border-radius: 20px;
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
    margin: 0;
}

.input-item {
    --background: #f8fafc;
    --border-radius: 12px;
    margin-bottom: 16px;
    border-radius: 12px;
    border: 2px solid transparent;
    transition: all 0.3s ease;
}

.input-item:focus-within {
    --background: white;
    border-color: #3b82f6;
    box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.1);
}

.input-icon {
    color: #64748b;
    font-size: 20px;
    margin-right: 12px;
}

.password-toggle {
    --color: #64748b;
    margin: 0;
}

.error-text {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 14px;
    margin: 12px 0;
    padding: 12px;
    background: #fee2e2;
    border-radius: 8px;
    border-left: 4px solid #ef4444;
}

.error-text ion-icon {
    font-size: 20px;
}

.login-button {
    --background: linear-gradient(135deg, #1e3a8a 0%, #3b82f6 100%);
    --border-radius: 12px;
    --box-shadow: 0 4px 16px rgba(59, 130, 246, 0.4);
    margin-top: 24px;
    height: 50px;
    font-weight: 600;
    text-transform: none;
    font-size: 16px;
    transition: all 0.3s ease;
}

.login-button:hover {
    --box-shadow: 0 6px 20px rgba(59, 130, 246, 0.5);
    transform: translateY(-2px);
}

.login-button:active {
    transform: translateY(0);
}

.forgot-password {
    text-align: center;
    margin-top: 16px;
}

.forgot-password ion-button {
    --color: #64748b;
    text-transform: none;
    font-size: 14px;
}

/* Divider */
.divider {
    display: flex;
    align-items: center;
    margin: 24px 0;
    text-align: center;
}

.divider::before,
.divider::after {
    content: '';
    flex: 1;
    border-bottom: 1px solid #e2e8f0;
}

.divider-text {
    padding: 0 16px;
    color: #94a3b8;
    font-size: 14px;
    font-weight: 500;
}

/* Google Button */
.google-button {
    --background: white;
    --color: #1f2937;
    --border-radius: 12px;
    --box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    height: 50px;
    font-weight: 600;
    text-transform: none;
    font-size: 16px;
    border: 2px solid #e5e7eb;
    transition: all 0.3s ease;
}

.google-button:hover {
    --background: #f9fafb;
    --box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
    transform: translateY(-2px);
}

.google-button:active {
    transform: translateY(0);
}

.google-button ion-icon {
    font-size: 24px;
    margin-right: 8px;
}

/* Guest Button */
.guest-button {
    --background: transparent;
    --color: #64748b;
    --border-radius: 12px;
    --border-color: #64748b;
    --border-width: 2px;
    height: 50px;
    font-weight: 600;
    text-transform: none;
    font-size: 16px;
    margin-top: 12px;
    transition: all 0.3s ease;
}

.guest-button:hover {
    --background: rgba(100, 116, 139, 0.1);
    --border-color: #475569;
    --color: #475569;
    transform: translateY(-2px);
}

.guest-button:active {
    transform: translateY(0);
}

/* Register Section */
.register-section {
    text-align: center;
    margin-top: 24px;
}

.register-text {
    color: rgba(255, 255, 255, 0.8);
    font-size: 14px;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 4px;
}

.register-button {
    --color: #3b82f6;
    text-transform: none;
    font-weight: 600;
    font-size: 14px;
    margin: 0;
}

/* Animations */
@keyframes fadeInDown {
    from {
        opacity: 0;
        transform: translateY(-30px);
    }

    to {
        opacity: 1;
        transform: translateY(0);
    }
}

@keyframes fadeInUp {
    from {
        opacity: 0;
        transform: translateY(30px);
    }

    to {
        opacity: 1;
        transform: translateY(0);
    }
}

/* Responsive */
@media (min-width: 768px) {
    .login-container {
        max-width: 440px;
        margin: 0 auto;
    }
}
</style>