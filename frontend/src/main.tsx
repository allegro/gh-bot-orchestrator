import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import "./index.css";
import "@mantine/core/styles.css";
import "@mantine/notifications/styles.css";
import { ColorSchemeScript, createTheme, MantineColorsTuple, MantineProvider } from "@mantine/core";
import App from "./App.tsx";
import { Notifications } from "@mantine/notifications";
import i18n from "i18next";
import Backend from "i18next-http-backend/cjs";
import LanguageDetector from "i18next-browser-languagedetector";
import { initReactI18next } from "react-i18next";
import { ModalsProvider } from "@mantine/modals";

const blue: MantineColorsTuple = ["#f0f5fa", "#e1eaf3", "#c3d5e7", "#a3bfdb", "#89abd1", "#769eca", "#6b96c7", "#5a7fb0", "#4f709e", "#41628c"];

const theme = createTheme({
    primaryColor: "blue",
    colors: {
        blue,
    },
    defaultGradient: {
        from: "blue.7",
        to: "blue.5",
        deg: 45,
    },
    components: {
        Button: {
            defaultProps: {
                color: "blue.7",
            },
        },
        Badge: {
            defaultProps: {
                color: "blue.5",
            },
        },
    },
});

i18n
    // load translation using http -> see /public/locales
    // learn more: https://github.com/i18next/i18next-http-backend
    .use(Backend)
    // detect user language
    // learn more: https://github.com/i18next/i18next-browser-languageDetector
    .use(LanguageDetector)
    // pass the i18n instance to react-i18next.
    .use(initReactI18next)
    // init i18next
    // for all options read: https://www.i18next.com/overview/configuration-options
    .init({
        supportedLngs: ["en", "pl"],
        fallbackLng: "en",
        debug: true,
    })
    .then(() => {
        createRoot(document.getElementById("root")!).render(
            <StrictMode>
                <ColorSchemeScript defaultColorScheme="auto" />
                <MantineProvider theme={theme} defaultColorScheme="auto">
                    <ModalsProvider>
                        <App />
                    </ModalsProvider>
                    <Notifications />
                </MantineProvider>
            </StrictMode>,
        );
    });
