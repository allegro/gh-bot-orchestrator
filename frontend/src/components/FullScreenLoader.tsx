import { Loader, Stack } from "@mantine/core";

export default function FullScreenLoader() {
    return (
        <Stack mih="80vh" h="100%" align="center" justify="center" gap="md">
            <Loader />
        </Stack>
    );
}
