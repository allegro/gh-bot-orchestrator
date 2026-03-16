import { Container, Image, Stack, Text } from "@mantine/core";

export function NotFoundScreen() {
    return (
        <Container size="sm">
            <Stack gap="lg" align="center" p="xl">
                <Image src="/undraw_page-not-found_6wni.svg" alt="nie znaleziono" />
                <Text size="lg" style={{ textWrap: "balance" }}>
                    Page not found
                </Text>
            </Stack>
        </Container>
    );
}
