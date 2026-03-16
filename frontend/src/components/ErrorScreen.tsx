import { Container, Image, Stack, Text } from "@mantine/core";

export function ErrorScreen({ error }: { error: Error }) {
    return (
        <Container size="sm">
            <Stack gap="lg" align="center" p="xl">
                <Image src="/undraw_server-error_syuz.svg" alt="error" />
                <Text size="lg" style={{ textWrap: "balance" }} ta="center">
                    {error.message}
                </Text>
            </Stack>
        </Container>
    );
}
