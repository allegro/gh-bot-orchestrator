import { createRootRouteWithContext, Outlet } from "@tanstack/react-router";
import { AppShell } from "@mantine/core";
import { ReactNode } from "react";
import { TanStackRouterDevtools } from "@tanstack/react-router-devtools";
import type { QueryClient } from "@tanstack/react-query";
import { ErrorBoundary } from "react-error-boundary";
import { ErrorScreen } from "../components/ErrorScreen.tsx";
import { NotFoundScreen } from "../components/NotFoundScreen.tsx";
import { AppHeader } from "../components/AppHeader.tsx";

interface RouterContext {
    queryClient: QueryClient;
}

export const Route = createRootRouteWithContext<RouterContext>()({
    component: () => (
        <Layout>
            <Outlet />
            <TanStackRouterDevtools />
        </Layout>
    ),
    notFoundComponent: NotFoundScreen,
});

function Layout({ children }: { children: ReactNode }) {
    return (
        <AppShell header={{ height: 60 }} padding="md">
            <AppShell.Header
                style={{
                    background: "linear-gradient(to right, #41628c, #5a7fb0)",
                    boxShadow: "0 2px 10px rgba(0, 0, 0, 0.1)",
                }}
            >
                <AppHeader />
            </AppShell.Header>
            <AppShell.Main>
                <ErrorBoundary fallbackRender={ErrorScreen}>{children}</ErrorBoundary>
            </AppShell.Main>
        </AppShell>
    );
}
