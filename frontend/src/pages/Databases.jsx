import { useEffect, useState } from 'react';
import { Database, Server, Activity, RefreshCw, Plus } from 'lucide-react';

export default function Databases() {
    const [databases, setDatabases] = useState([]);
    const [loading, setLoading] = useState(true);
    const [detectingId, setDetectingId] = useState(null);

    const fetchDatabases = () => {
        fetch('http://localhost:8080/api/databases')
            .then((res) => res.json())
            .then((data) => {
                setDatabases(data);
                setLoading(false);
            })
            .catch((err) => {
                console.error("Error al cargar bases de datos:", err);
                setLoading(false);
            });
    };

    useEffect(() => {
        fetchDatabases();
    }, []);

    const handleDetectMode = (id) => {
        setDetectingId(id);
        fetch(`http://localhost:8080/api/databases/${id}/detect-archive-mode`, {
            method: 'POST'
        })
            .then((res) => res.json())
            .then(() => {
                fetchDatabases(); // Recargamos la lista para ver el nuevo modo
            })
            .catch((err) => console.error("Error al detectar modo:", err))
            .finally(() => setDetectingId(null));
    };

    return (
        <div className="max-w-6xl mx-auto space-y-8 animate-in fade-in duration-500">
            <div className="flex justify-between items-end">
                <div>
                    <h2 className="text-3xl font-semibold tracking-tight text-gray-900">Bases de Datos</h2>
                    <p className="text-gray-500 mt-2">Gestione los objetivos (targets) para sus estrategias de respaldo.</p>
                </div>
                <button className="flex items-center gap-2 bg-blue-600 hover:bg-blue-700 text-white px-4 py-2.5 rounded-xl font-medium transition-colors shadow-sm">
                    <Plus size={20} />
                    Registrar Base de Datos
                </button>
            </div>

            <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
                {loading ? (
                    <div className="flex justify-center py-12">
                        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
                    </div>
                ) : databases.length === 0 ? (
                    <div className="text-center py-12 text-gray-500">No hay bases de datos registradas.</div>
                ) : (
                    <table className="w-full text-left border-collapse">
                        <thead>
                        <tr className="bg-gray-50/50 border-b border-gray-100">
                            <th className="px-6 py-4 text-sm font-medium text-gray-500">Nombre / Host</th>
                            <th className="px-6 py-4 text-sm font-medium text-gray-500">Servicio</th>
                            <th className="px-6 py-4 text-sm font-medium text-gray-500">Prioridad</th>
                            <th className="px-6 py-4 text-sm font-medium text-gray-500">Modo Archive</th>
                            <th className="px-6 py-4 text-sm font-medium text-gray-500 text-right">Acciones</th>
                        </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-100">
                        {databases.map((db) => (
                            <tr key={db.id} className="hover:bg-gray-50/50 transition-colors">
                                <td className="px-6 py-4">
                                    <div className="flex items-center gap-3">
                                        <div className="p-2 bg-blue-50 text-blue-600 rounded-lg">
                                            <Database size={20} />
                                        </div>
                                        <div>
                                            <div className="font-semibold text-gray-900">{db.name}</div>
                                            <div className="text-sm text-gray-500 flex items-center gap-1 mt-0.5">
                                                <Server size={14} /> {db.host}:{db.port}
                                            </div>
                                        </div>
                                    </div>
                                </td>
                                <td className="px-6 py-4 text-gray-700 font-medium">{db.serviceName}</td>
                                <td className="px-6 py-4">
                    <span className={`inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium border
                      ${db.priority === 'ALTA' ? 'bg-red-50 text-red-700 border-red-100' :
                        db.priority === 'MEDIA' ? 'bg-yellow-50 text-yellow-700 border-yellow-100' :
                            'bg-green-50 text-green-700 border-green-100'}`}>
                      {db.priority}
                    </span>
                                </td>
                                <td className="px-6 py-4">
                                    <div className="flex items-center gap-2">
                                        <Activity size={16} className={db.archiveMode === 'ARCHIVELOG' ? 'text-green-500' : 'text-gray-400'} />
                                        <span className="text-sm font-medium text-gray-700">{db.archiveMode}</span>
                                    </div>
                                </td>
                                <td className="px-6 py-4 text-right">
                                    <button
                                        onClick={() => handleDetectMode(db.id)}
                                        disabled={detectingId === db.id}
                                        className="inline-flex items-center gap-2 text-sm font-medium text-blue-600 hover:text-blue-700 bg-blue-50 hover:bg-blue-100 px-3 py-1.5 rounded-lg transition-colors disabled:opacity-50"
                                    >
                                        <RefreshCw size={16} className={detectingId === db.id ? "animate-spin" : ""} />
                                        {detectingId === db.id ? 'Detectando...' : 'Detectar Modo'}
                                    </button>
                                </td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                )}
            </div>
        </div>
    );
}