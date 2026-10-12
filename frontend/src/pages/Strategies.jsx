import { useEffect, useState } from 'react';
import { Shield, Plus, Play, FileCode2, Database, Clock, CheckCircle2, XCircle } from 'lucide-react';

export default function Strategies() {
    const [strategies, setStrategies] = useState([]);
    const [loading, setLoading] = useState(true);

    const fetchStrategies = () => {
        fetch('http://localhost:8080/api/strategies')
            .then((res) => res.json())
            .then((data) => {
                setStrategies(data);
                setLoading(false);
            })
            .catch((err) => {
                console.error("Error al cargar estrategias:", err);
                setLoading(false);
            });
    };

    useEffect(() => {
        fetchStrategies();
    }, []);

    const handleRun = (id, name) => {
        if (window.confirm(`¿Deseas iniciar un respaldo manual (Tipo FULL) para la estrategia '${name}'?`)) {
            fetch(`http://localhost:8080/api/strategies/${id}/run?backupType=FULL`, {
                method: 'POST'
            })
                .then(() => alert('✅ Ejecución finalizada. Revisa la pestaña de Historial para ver la evidencia.'))
                .catch((err) => console.error("Error al ejecutar:", err));
        }
    };

    const handlePreviewScript = (id) => {
        fetch(`http://localhost:8080/api/strategies/${id}/script?backupType=FULL`)
            .then((res) => res.json())
            .then((data) => {
                // En un futuro puedes cambiar este alert por un Modal bonito
                alert("VISTA PREVIA SCRIPT RMAN:\n\n" + data.script);
            })
            .catch((err) => console.error("Error al previsualizar:", err));
    };

    return (
        <div className="max-w-6xl mx-auto space-y-8 animate-in fade-in duration-500">
            <div className="flex justify-between items-end">
                <div>
                    <h2 className="text-3xl font-semibold tracking-tight text-gray-900">Estrategias de Respaldo</h2>
                    <p className="text-gray-500 mt-2">Configure qué, cómo y cuándo se respaldan sus bases de datos.</p>
                </div>
                <button className="flex items-center gap-2 bg-blue-600 hover:bg-blue-700 text-white px-4 py-2.5 rounded-xl font-medium transition-colors shadow-sm">
                    <Plus size={20} />
                    Nueva Estrategia
                </button>
            </div>

            <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
                {loading ? (
                    <div className="flex justify-center py-12">
                        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
                    </div>
                ) : strategies.length === 0 ? (
                    <div className="text-center py-12 text-gray-500">No hay estrategias configuradas.</div>
                ) : (
                    <table className="w-full text-left border-collapse">
                        <thead>
                        <tr className="bg-gray-50/50 border-b border-gray-100">
                            <th className="px-6 py-4 text-sm font-medium text-gray-500">Estrategia</th>
                            <th className="px-6 py-4 text-sm font-medium text-gray-500">Retención</th>
                            <th className="px-6 py-4 text-sm font-medium text-gray-500">Estado</th>
                            <th className="px-6 py-4 text-sm font-medium text-gray-500 text-right">Acciones Rápidas</th>
                        </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-100">
                        {strategies.map((st) => (
                            <tr key={st.id} className="hover:bg-gray-50/50 transition-colors">
                                <td className="px-6 py-4">
                                    <div className="flex items-center gap-3">
                                        <div className="p-2 bg-indigo-50 text-indigo-600 rounded-lg">
                                            <Shield size={20} />
                                        </div>
                                        <div>
                                            <div className="font-semibold text-gray-900">{st.name}</div>
                                            <div className="text-sm text-gray-500 mt-0.5">{st.description}</div>
                                        </div>
                                    </div>
                                </td>
                                <td className="px-6 py-4">
                                    <div className="flex items-center gap-1.5 text-gray-700 font-medium">
                                        <Clock size={16} className="text-gray-400" />
                                        {st.retentionDays} días
                                    </div>
                                </td>
                                <td className="px-6 py-4">
                                    {st.active ? (
                                        <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium bg-green-50 text-green-700 border border-green-100">
                        <CheckCircle2 size={14} /> Activa
                      </span>
                                    ) : (
                                        <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium bg-gray-100 text-gray-700 border border-gray-200">
                        <XCircle size={14} /> Inactiva
                      </span>
                                    )}
                                </td>
                                <td className="px-6 py-4 text-right">
                                    <div className="flex justify-end gap-2">
                                        <button
                                            onClick={() => handlePreviewScript(st.id)}
                                            className="inline-flex items-center gap-2 text-sm font-medium text-gray-600 hover:text-gray-900 bg-gray-50 hover:bg-gray-100 px-3 py-1.5 rounded-lg transition-colors"
                                            title="Ver Script RMAN"
                                        >
                                            <FileCode2 size={16} />
                                            Script
                                        </button>
                                        <button
                                            onClick={() => handleRun(st.id, st.name)}
                                            className="inline-flex items-center gap-2 text-sm font-medium text-blue-600 hover:text-blue-700 bg-blue-50 hover:bg-blue-100 px-3 py-1.5 rounded-lg transition-colors"
                                        >
                                            <Play size={16} fill="currentColor" />
                                            Ejecutar
                                        </button>
                                    </div>
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