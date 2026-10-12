import { useEffect, useState } from 'react';
import { Database, Server, Activity, RefreshCw, Plus, X, Trash2 } from 'lucide-react';

export default function Databases() {
    const [databases, setDatabases] = useState([]);
    const [loading, setLoading] = useState(true);
    const [detectingId, setDetectingId] = useState(null);

    const [isModalOpen, setIsModalOpen] = useState(false);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [formData, setFormData] = useState({
        name: '',
        host: 'localhost',
        port: 1521,
        serviceName: 'XE',
        dbUser: 'sys',
        password: '',
        priority: 'MEDIA',
        active: true
    });

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
        fetch(`http://localhost:8080/api/databases/${id}/detect-archive-mode`, { method: 'POST' })
            .then(async (res) => {
                if (!res.ok) {
                    const errData = await res.json();
                    throw new Error(errData.message || 'Error al conectar con Oracle');
                }
                return res.json();
            })
            .then((data) => {
                alert(`Conexión exitosa. El modo actual es: ${data.archiveMode}`);
                fetchDatabases();
            })
            .catch((err) => {
                alert(`Falló la conexión a la base de datos real:\n\n${err.message}\n\nRevisa que la IP, puerto y credenciales sean correctas.`);
            })
            .finally(() => setDetectingId(null));
    };

    const handleDelete = (id, name) => {
        if (window.confirm(`¿Estás seguro de que deseas eliminar la base de datos '${name}'?\nEsta acción no se puede deshacer.`)) {
            fetch(`http://localhost:8080/api/databases/${id}`, { method: 'DELETE' })
                .then(async (res) => {
                    if (!res.ok) {
                        const errData = await res.json().catch(() => ({}));
                        throw new Error(errData.message || 'No se puede eliminar la base de datos. Es posible que tenga estrategias asociadas.');
                    }
                    fetchDatabases();
                })
                .catch((err) => alert(err.message));
        }
    };

    const handleInputChange = (e) => {
        const { name, value, type, checked } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: type === 'checkbox' ? checked : (type === 'number' ? Number(value) : value)
        }));
    };

    const handleSubmit = (e) => {
        e.preventDefault();
        setIsSubmitting(true);

        fetch('http://localhost:8080/api/databases', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(formData)
        })
            .then(async (res) => {
                if (!res.ok) {
                    const errorData = await res.json();
                    throw new Error(errorData.message || 'Error al registrar la base de datos');
                }
                return res.json();
            })
            .then(() => {
                setIsModalOpen(false);
                fetchDatabases();
                setFormData({ name: '', host: 'localhost', port: 1521, serviceName: 'XE', dbUser: 'sys', password: '', priority: 'MEDIA', active: true });
            })
            .catch((err) => alert(err.message))
            .finally(() => setIsSubmitting(false));
    };

    return (
        <div className="max-w-6xl mx-auto space-y-8 animate-in fade-in duration-500">
            <div className="flex justify-between items-end">
                <div>
                    <h2 className="text-3xl font-semibold tracking-tight text-gray-900">Bases de Datos</h2>
                    <p className="text-gray-500 mt-2">Gestione los objetivos (targets) para sus estrategias de respaldo.</p>
                </div>
                <button
                    onClick={() => setIsModalOpen(true)}
                    className="flex items-center gap-2 bg-blue-600 hover:bg-blue-700 text-white px-4 py-2.5 rounded-xl font-medium transition-colors shadow-sm"
                >
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
                                    <div className="flex justify-end gap-2">
                                        <button
                                            onClick={() => handleDetectMode(db.id)}
                                            disabled={detectingId === db.id}
                                            className="inline-flex items-center gap-2 text-sm font-medium text-blue-600 hover:text-blue-700 bg-blue-50 hover:bg-blue-100 px-3 py-1.5 rounded-lg transition-colors disabled:opacity-50"
                                            title="Probar conexión con Oracle"
                                        >
                                            <RefreshCw size={16} className={detectingId === db.id ? "animate-spin" : ""} />
                                            Detectar Modo
                                        </button>
                                        <button
                                            onClick={() => handleDelete(db.id, db.name)}
                                            className="inline-flex items-center justify-center text-red-500 hover:text-red-700 bg-red-50 hover:bg-red-100 p-1.5 rounded-lg transition-colors"
                                            title="Eliminar Base de Datos"
                                        >
                                            <Trash2 size={18} />
                                        </button>
                                    </div>
                                </td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                )}
            </div>

            {isModalOpen && (
                <div className="fixed inset-0 bg-black/40 backdrop-blur-sm z-50 flex justify-center items-center p-4">
                    <div className="bg-white rounded-2xl w-full max-w-lg shadow-xl overflow-hidden animate-in zoom-in-95 duration-200">
                        <div className="flex justify-between items-center p-6 border-b border-gray-100">
                            <h3 className="text-lg font-semibold text-gray-900">Registrar Base de Datos</h3>
                            <button onClick={() => setIsModalOpen(false)} className="text-gray-400 hover:text-gray-600 transition-colors">
                                <X size={20} />
                            </button>
                        </div>

                        <form onSubmit={handleSubmit} className="p-6 space-y-4">
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">Nombre Identificador</label>
                                <input type="text" name="name" required value={formData.name} onChange={handleInputChange} placeholder="Ej. Produccion_Oracle"
                                       className="w-full px-4 py-2 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all" />
                            </div>

                            <div className="grid grid-cols-3 gap-4">
                                <div className="col-span-2">
                                    <label className="block text-sm font-medium text-gray-700 mb-1">Host (IP o Dominio)</label>
                                    <input type="text" name="host" required value={formData.host} onChange={handleInputChange}
                                           className="w-full px-4 py-2 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 transition-all" />
                                </div>
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">Puerto</label>
                                    <input type="number" name="port" required min="1" max="65535" value={formData.port} onChange={handleInputChange}
                                           className="w-full px-4 py-2 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 transition-all" />
                                </div>
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">Service Name (SID)</label>
                                <input type="text" name="serviceName" required value={formData.serviceName} onChange={handleInputChange}
                                       className="w-full px-4 py-2 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 transition-all" />
                            </div>

                            <div className="grid grid-cols-2 gap-4">
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">Usuario DB (sysdba)</label>
                                    <input type="text" name="dbUser" required value={formData.dbUser} onChange={handleInputChange}
                                           className="w-full px-4 py-2 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 transition-all" />
                                </div>
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">Contraseña</label>
                                    <input type="password" name="password" required value={formData.password} onChange={handleInputChange}
                                           className="w-full px-4 py-2 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 transition-all" />
                                </div>
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">Prioridad</label>
                                <select name="priority" value={formData.priority} onChange={handleInputChange}
                                        className="w-full px-4 py-2 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 bg-white transition-all">
                                    <option value="ALTA">ALTA</option>
                                    <option value="MEDIA">MEDIA</option>
                                    <option value="BAJA">BAJA</option>
                                </select>
                            </div>

                            <div className="pt-4 flex justify-end gap-3">
                                <button type="button" onClick={() => setIsModalOpen(false)}
                                        className="px-4 py-2 text-gray-700 bg-gray-50 hover:bg-gray-100 rounded-xl font-medium transition-colors">
                                    Cancelar
                                </button>
                                <button type="submit" disabled={isSubmitting}
                                        className="px-6 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-xl font-medium transition-colors shadow-sm disabled:opacity-50">
                                    {isSubmitting ? 'Guardando...' : 'Guardar Base de Datos'}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}