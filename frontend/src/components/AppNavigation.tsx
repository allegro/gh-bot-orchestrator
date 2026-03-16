import { NavLink } from "@mantine/core";
import { Link } from "@tanstack/react-router";
import { useTranslation } from "react-i18next";

export function AppNavigation({ toggleMobile, toggleDesktop }: { toggleMobile: () => void; toggleDesktop: () => void }) {
    const { t } = useTranslation();
    const handleClick = () => {
        toggleMobile();
        toggleDesktop();
    };

    return (
        <nav>
            <NavLink component={Link} to="/" label={t("dashboard.title")} onClick={handleClick} />
            <NavLink component={Link} to="/about" label={t("about.title")} onClick={handleClick} />
        </nav>
    );
}
