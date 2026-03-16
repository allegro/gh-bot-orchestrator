import { routeTree } from "./routeTree.gen";
import { createRouter, RouterProvider } from "@tanstack/react-router";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";

const queryClient = new QueryClient({
    defaultOptions: {
        queries: {
            retry: false,
        },
    },
});

const app = createRouter({
    routeTree,
    context: {
        queryClient,
    },
    defaultPreload: "intent",
    // Since we're using React Query, we don't want loader calls to ever be stale
    // This will ensure that the loader is always called when the route is preloaded or visited
    defaultPreloadStaleTime: 0,
    scrollRestoration: true,
});

// Register the app instance for type safety
declare module "@tanstack/react-router" {
    interface Register {
        router: typeof app;
    }
}

export default function App() {
    return (
        <QueryClientProvider client={queryClient}>
            <RouterProvider router={app} />
        </QueryClientProvider>
    );
}
