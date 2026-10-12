import { BrowserRouter, Routes, Route, NavLink } from 'react-router-dom';
// Aquí le ponemos el alias "HistoryIcon" al ícono para evitar el choque
import { LayoutDashboard, Database, Shield, History as HistoryIcon } from 'lucide-react';
import Dashboard from './pages/Dashboard';
import Databases from './pages/Databases';
import Strategies from './pages/Strategies';
import History from './pages/History';

function Sidebar() {
    const navItems = [
        { to: '/', icon: LayoutDashboard, label: 'Dashboard' },
        { to: '/databases', icon: Database, label: 'Bases de Datos' },
        { to: '/strategies', icon: Shield, label: 'Estrategias' },
        // Y aquí usamos el HistoryIcon
        { to: '/history', icon: HistoryIcon, label: 'Historial' },
    ];

    return (
        <div className="w-64 bg-white border-r border-gray-100 h-screen fixed left-0 top-0 flex flex-col">
            <div className="p-6">
                <h1 className="text-xl font-bold bg-gradient-to-r from-blue-600 to-indigo-600 bg-clip-text text-transparent">
                    RMAN Manager
                </h1>
            </div>
            <nav className="flex-1 px-4 space-y-2 mt-4">
                {navItems.map((item) => (
                    <NavLink
                        key={item.to}
                        to={item.to}
                        className={({ isActive }) =>
                            `flex items-center gap-3 px-4 py-3 rounded-xl transition-all duration-200 ${
                                isActive
                                    ? 'bg-blue-50 text-blue-600 font-medium'
                                    : 'text-gray-500 hover:bg-gray-50 hover:text-gray-900'
                            }`
                        }
                    >
                        {({ isActive }) => (
                            <>
                                <item.icon size={20} strokeWidth={isActive ? 2.5 : 2} />
                                {item.label}
                            </>
                        )}
                    </NavLink>
                ))}
            </nav>
        </div>
    );
}

export default function App() {
    return (
        <BrowserRouter>
            <div className="flex min-h-screen bg-[#fbfbfd]">
                <Sidebar />
                <main className="flex-1 ml-64 p-8">
                    <Routes>
                        <Route path="/" element={<Dashboard />} />
                        <Route path="/databases" element={<Databases />} />
                        <Route path="/strategies" element={<Strategies />} />
                        <Route path="/history" element={<History />} />
                    </Routes>
                </main>
            </div>
        </BrowserRouter>
    );
}